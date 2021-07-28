/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.LivingEntityMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
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
    
    private final Setting<Boolean> flight = sgGeneral.add(new BoolSetting.Builder()
            .name("flight")
            .description("Fly with entity.")
            .defaultValue(false)
            .build()
    );
    
    private final Setting<Double> flySpeed = sgGeneral.add(new DoubleSetting.Builder()
    		.name("vertical-speed")
    		.description("Vertical speed while flying.")
    		.defaultValue(0.5)
    		.min(0)
    		.build());
    
    private final Setting<Double> gravity = sgGeneral.add(new DoubleSetting.Builder()
    		.name("gravity")
    		.description("Strength of gravity while flying.")
    		.defaultValue(0.5)
    		.min(0)
    		.build());
    
    private final Setting<Boolean> gravityInWater = sgGeneral.add(new BoolSetting.Builder()
            .name("gravity-in-water")
            .description("Gravity affects flight while in water.")
            .defaultValue(false)
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
    
    @Override
    public String getInfoString() {
        return flight.get() ? "Flight" : "Normal";
    }

    @EventHandler
    private void onLivingEntityMove(LivingEntityMoveEvent event) {
        if (event.entity.getPrimaryPassenger() != mc.player) return;

        // Set horizontal velocity
        Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
        LivingEntity entity = event.entity;
        if(!flight.get()) {
            // Check for onlyOnGround and inWater
            if (onlyOnGround.get() && !entity.isOnGround()) return;
            if (!inWater.get() && entity.isTouchingWater()) return;
            
            ((IVec3d) event.movement).setXZ(vel.x, vel.z);
        }else {
            double fallspeed = -(gravity.get());
            if (!gravityInWater.get() && entity.isTouchingWater()) fallspeed = 0;
            if (mc.options.keyJump.isPressed()) fallspeed = flySpeed.get();
            if (mc.options.keySprint.isPressed()) fallspeed = -(flySpeed.get());
            ((IVec3d) event.movement).set(vel.x, fallspeed, vel.z);
        }
    }
}
