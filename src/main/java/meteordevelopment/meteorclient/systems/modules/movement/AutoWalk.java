/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.NopPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.option.KeyBinding;

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
            switch (direction.get()) {
                case Forwards -> mc.options.forwardKey.setPressed(true);
                case Backwards -> mc.options.backKey.setPressed(true);
                case Left -> mc.options.leftKey.setPressed(true);
                case Right -> mc.options.rightKey.setPressed(true);
            }
        } else {
            if (PathManagers.get() instanceof NopPathManager) {
                info("Smart mode requires Baritone");
                toggle();
            }
        }
    }

    private void unpress() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
    }

    private void createGoal() {
        PathManagers.get().moveInDirection(mc.player.getYaw());
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
