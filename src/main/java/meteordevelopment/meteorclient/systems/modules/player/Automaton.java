/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;

/**
 * Plays a string as a script of player movements.
 *
 * <p>The script is a sequence of single character commands, one per character. Every command
 * toggles the matching control:
 *
 * <pre>
 * 0 move (master switch)    5 left
 * 1 attack                  6 right
 * 2 use                     7 sneak
 * 3 forward                 8 jump
 * 4 back
 * </pre>
 *
 * The controls are written to the vanilla key bindings every tick, so the player moves while the
 * real keys are not pressed at all.
 *
 * <p>{@code column} is the cursor into the script. With <i>stepper</i> enabled the cursor moves by
 * <i>step direction</i> every tick, which is what plays the script forward over time.
 */
public class Automaton extends Module {
    private final SettingGroup sgExecute = settings.createGroup("Execute");
    private final SettingGroup sgControl = settings.createGroup("Control");
    private final SettingGroup sgScript = settings.createGroup("Script");

    // Execute

    public final Setting<Boolean> pre = sgExecute.add(new BoolSetting.Builder()
        .name("pre")
        .description("Executes the script before the tick.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> post = sgExecute.add(new BoolSetting.Builder()
        .name("post")
        .description("Executes the script after the tick.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> debug = sgExecute.add(new BoolSetting.Builder()
        .name("debug")
        .description("Prints script errors to the log.")
        .defaultValue(false)
        .build()
    );

    // Script

    public final Setting<String> script = sgScript.add(new StringSetting.Builder()
        .name("script")
        .description("The script to play. Every character is a command, see the module description.")
        .defaultValue("")
        .build()
    );

    public final Setting<Boolean> handler = sgScript.add(new BoolSetting.Builder()
        .name("handler")
        .description("Applies every character of the script as a command on each tick, instead of only the one under the cursor.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> stepper = sgScript.add(new BoolSetting.Builder()
        .name("stepper")
        .description("Moves the cursor through the script every tick.")
        .defaultValue(true)
        .build()
    );

    public final Setting<StepDirections> stepDirections = sgScript.add(new EnumSetting.Builder<StepDirections>()
        .name("step-direction")
        .description("The direction the cursor moves in.")
        .defaultValue(StepDirections.Increment)
        .build()
    );

    public final Setting<Integer> column = sgScript.add(new IntSetting.Builder()
        .name("column")
        .description("The cursor position the reset button restores.")
        .defaultValue(0)
        .min(0)
        .sliderMax(64)
        .build()
    );

    // Control

    public final Setting<Boolean> move = sgControl.add(new BoolSetting.Builder()
        .name("move")
        .description("Whether player movement is driven by the script at all.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> attack = sgControl.add(new BoolSetting.Builder()
        .name("attack")
        .description("Attacks.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> use = sgControl.add(new BoolSetting.Builder()
        .name("use")
        .description("Uses the held item.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> forward = sgControl.add(new BoolSetting.Builder()
        .name("forward")
        .description("Walks forwards.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> back = sgControl.add(new BoolSetting.Builder()
        .name("back")
        .description("Walks backwards.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> left = sgControl.add(new BoolSetting.Builder()
        .name("left")
        .description("Walks left.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> right = sgControl.add(new BoolSetting.Builder()
        .name("right")
        .description("Walks right.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> jump = sgControl.add(new BoolSetting.Builder()
        .name("jump")
        .description("Jumps.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> sneak = sgControl.add(new BoolSetting.Builder()
        .name("sneak")
        .description("Sneaks.")
        .defaultValue(false)
        .build()
    );

    private int c;
    private WLabel columnLabel;

    public Automaton() {
        super(Categories.Player, "automaton", "Allows scripted inaccurate moving without presence at the keyboard.");
    }

    @Override
    public void onDeactivate() {
        release();
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (pre.get()) engine();
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (post.get()) engine();
        if (columnLabel != null) columnLabel.set("Cursor: " + c);
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        if (!Utils.canUpdate()) return theme.label("You need to be in a world.");

        WTable table = theme.table();
        initTable(theme, table);
        return table;
    }

    /** Plays one step of the script. */
    private void engine() {
        try {
            String script = this.script.get();

            translate(script, handler.get());
            applyMovement();
            step(stepper.get(), script);
        } catch (Exception e) {
            if (debug.get()) e.printStackTrace();
        }
    }

    /**
     * Resolves the script into commands and applies them.
     *
     * <p>With <i>handler</i> on, the whole script is resolved on this tick ("string to char, char
     * to command"); with it off only the character under the cursor is, which is what makes the
     * script play forward over time.
     */
    private void translate(String script, boolean handler) {
        if (handler) {
            for (int i = 0; i < script.length(); i++) translate(script.charAt(i));
        } else if (!script.isEmpty()) {
            translate(script.charAt(Math.floorMod(c, script.length())));
        }
    }

    /** Applies a single script command. */
    private void translate(char command) {
        switch (command) {
            case '0' -> move.set(!move.get());
            case '1' -> attack.set(!attack.get());
            case '2' -> use.set(!use.get());
            case '3' -> forward.set(!forward.get());
            case '4' -> back.set(!back.get());
            case '5' -> left.set(!left.get());
            case '6' -> right.set(!right.get());
            case '7' -> sneak.set(!sneak.get());
            case '8' -> jump.set(!jump.get());
        }
    }

    /** Moves the cursor through the script. */
    private void step(boolean stepper, String script) {
        if (!stepper || script.isEmpty()) return;

        switch (stepDirections.get()) {
            case None -> {}
            case Increment -> c++;
            case Decrement -> c--;
        }
    }

    /** Writes the current controls to the vanilla key bindings. */
    private void applyMovement() {
        boolean enabled = move.get();

        mc.options.keyUp.setDown(enabled && forward.get());
        mc.options.keyDown.setDown(enabled && back.get());
        mc.options.keyLeft.setDown(enabled && left.get());
        mc.options.keyRight.setDown(enabled && right.get());
        mc.options.keyJump.setDown(enabled && jump.get());
        mc.options.keyShift.setDown(enabled && sneak.get());
        mc.options.keyAttack.setDown(enabled && attack.get());
        mc.options.keyUse.setDown(enabled && use.get());
    }

    /** Releases every key this module might have pressed. */
    private void release() {
        if (mc == null || mc.options == null) return;

        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
    }

    public void setColumn(int column) {
        c = Math.max(0, column);
    }

    private void initTable(GuiTheme theme, WTable table) {
        table.clear();

        table.add(theme.horizontalSeparator()).expandX();
        table.row();

        columnLabel = theme.label("Cursor: " + c);
        table.add(columnLabel);

        WButton reset = table.add(theme.button("Reset")).expandX().widget();
        reset.action = () -> setColumn(column.get());
    }

    public enum StepDirections {
        None,
        Increment,
        Decrement
    }
}
