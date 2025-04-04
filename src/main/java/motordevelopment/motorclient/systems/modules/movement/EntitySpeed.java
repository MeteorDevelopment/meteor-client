/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.movement;

import motordevelopment.motorclient.events.entity.LivingEntityMoveEvent;
import motordevelopment.motorclient.mixininterface.IVec3d;
import motordevelopment.motorclient.settings.BoolSetting;
import motordevelopment.motorclient.settings.DoubleSetting;
import motordevelopment.motorclient.settings.Setting;
import motordevelopment.motorclient.settings.SettingGroup;
import motordevelopment.motorclient.systems.modules.Categories;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class EntitySpeed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Horizontal speed in blocks per second.")
        .defaultValue(10)
        .min(0)
        .sliderMax(50)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Use speed only when standing on a block.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> inWater = sgGeneral.add(new BoolSetting.Builder()
        .name("in-water")
        .description("Use speed when in water.")
        .defaultValue(false)
        .build()
    );

    public EntitySpeed() {
        super(Categories.Movement, "entity-speed", "Makes you go faster when riding entities.");
    }

    @EventHandler
    private void onLivingEntityMove(LivingEntityMoveEvent event) {
        if (event.entity.getControllingPassenger() != mc.player) return;

        // Check for onlyOnGround and inWater
        LivingEntity entity = event.entity;
        if (onlyOnGround.get() && !entity.isOnGround()) return;
        if (!inWater.get() && entity.isTouchingWater()) return;

        // Set horizontal velocity
        Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
        ((IVec3d) event.movement).motor$setXZ(vel.x, vel.z);
    }
}
