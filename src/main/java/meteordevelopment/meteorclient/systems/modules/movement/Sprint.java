/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayerEntityAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;

public class Sprint extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum Mode {
        Strict,
        Rage
    }

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("speed-mode")
        .description("What mode of sprinting.")
        .defaultValue(Mode.Strict)
        .build()
    );

    public final Setting<Boolean> jumpFix = sgGeneral.add(new BoolSetting.Builder()
        .name("jump-fix")
        .description("Whether to correct jumping directions.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Rage)
        .build()
    );

    private final Setting<Boolean> keepSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-sprint")
        .description("Whether to keep sprinting after attacking an entity.")
        .defaultValue(false)
        .build()
    );

    public Sprint() {
        super(Categories.Movement, "sprint", "Automatically sprints.");
    }

    @Override
    public void onDeactivate() {
        mc.player.setSprinting(false);
    }

    @EventHandler
    private void onTickMovement(TickEvent.Post event) {
        if (shouldSprint()) mc.player.setSprinting(true);
    }

    public boolean shouldSprint() {
        boolean strictSprint = mc.player.forwardSpeed > 1.0E-5F
            && ((ClientPlayerEntityAccessor) mc.player).invokeCanSprint()
            && (!mc.player.horizontalCollision || mc.player.collidedSoftly)
            && !(mc.player.isTouchingWater() && !mc.player.isSubmergedInWater());

        return isActive() && (mode.get() == Mode.Rage || strictSprint) && (mc.currentScreen == null || Modules.get().get(GUIMove.class).sprint.get());
    }

    public boolean rageSprint() {
        return isActive() && mode.get() == Mode.Rage;
    }

    public boolean stopSprinting() {
        return !isActive() || !keepSprint.get();
    }
}
