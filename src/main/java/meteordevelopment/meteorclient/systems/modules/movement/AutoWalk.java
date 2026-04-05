/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.pathing.NopPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class AutoWalk extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Walking mode.")
        .defaultValue(Mode.Smart)
        .onChanged(mode1 -> {
            if (isActive() && Utils.canUpdate()) {
                if (mode1 == Mode.Simple) {
                    PathManagers.get().stop();
                } else {
                    createGoal();
                }

                unpress();
            }
        })
        .build()
    );

    private final Setting<Direction> direction = sgGeneral.add(new EnumSetting.Builder<Direction>()
        .name("simple-direction")
        .description("The direction to walk in simple mode.")
        .defaultValue(Direction.Forwards)
        .onChanged(direction1 -> {
            if (isActive()) unpress();
        })
        .visible(() -> mode.get() == Mode.Simple)
        .build()
    );

    private final Setting<Boolean> disableOnInput = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-input")
        .description("Disable module on manual movement input")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> disableOnY = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-y-change")
        .description("Disable module if player moves vertically")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Simple)
        .build()
    );

    private final Setting<Boolean> waitForChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("no-unloaded-chunks")
        .description("Do not allow movement into unloaded chunks")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Simple)
        .build()
    );

    public AutoWalk() {
        super(Categories.Movement, "auto-walk", "Automatically walks forward.");
    }

    @Override
    public void onActivate() {
        if (mode.get() == Mode.Smart) createGoal();
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.Simple) unpress();
        else PathManagers.get().stop();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (mode.get() == Mode.Simple) {
            if (disableOnY.get() && mc.player.yo != mc.player.getY()) {
                toggle();
                return;
            }

            switch (direction.get()) {
                case Forwards -> mc.options.keyUp.setDown(true);
                case Backwards -> mc.options.keyDown.setDown(true);
                case Left -> mc.options.keyLeft.setDown(true);
                case Right -> mc.options.keyRight.setDown(true);
            }
        } else {
            if (PathManagers.get() instanceof NopPathManager) {
                info("Smart mode requires Baritone");
                toggle();
            }
        }
    }

    private void onMovement() {
        if (!disableOnInput.get()) return;
        if (mc.screen != null) {
            GUIMove guiMove = Modules.get().get(GUIMove.class);
            if (!guiMove.isActive()) return;
            if (guiMove.skip()) return;
        }
        toggle();
    }

    @EventHandler
    private void onKey(KeyInputEvent event) {
        if (isMovementKey(event.input) && event.action == KeyAction.Press) onMovement();
    }

    @EventHandler
    private void onMouseClick(MouseClickEvent event) {
        if (isMovementButton(event.click) && event.action == KeyAction.Press) onMovement();
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (mode.get() == Mode.Simple && waitForChunks.get()) {
            int chunkX = (int) ((mc.player.getX() + event.movement.x * 2) / 16);
            int chunkZ = (int) ((mc.player.getZ() + event.movement.z * 2) / 16);
            if (!mc.level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                ((IVec3) event.movement).meteor$set(0, event.movement.y, 0);
            }
        }
    }

    private void unpress() {
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
    }

    private boolean isMovementKey(KeyEvent input) {
        return mc.options.keyUp.matches(input)
            || mc.options.keyDown.matches(input)
            || mc.options.keyLeft.matches(input)
            || mc.options.keyRight.matches(input)
            || mc.options.keyShift.matches(input)
            || mc.options.keyJump.matches(input);
    }

    private boolean isMovementButton(MouseButtonEvent click) {
        return mc.options.keyUp.matchesMouse(click)
            || mc.options.keyDown.matchesMouse(click)
            || mc.options.keyLeft.matchesMouse(click)
            || mc.options.keyRight.matchesMouse(click)
            || mc.options.keyShift.matchesMouse(click)
            || mc.options.keyJump.matchesMouse(click);
    }

    private void createGoal() {
        PathManagers.get().moveInDirection(mc.player.getYRot());
    }

    public enum Mode {
        Simple,
        Smart
    }

    public enum Direction {
        Forwards,
        Backwards,
        Left,
        Right
    }
}
