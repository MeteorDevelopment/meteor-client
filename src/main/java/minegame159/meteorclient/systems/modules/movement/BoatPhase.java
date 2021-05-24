/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.BoatMoveEvent;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.utils.player.PlayerUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;

public class BoatPhase extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeeds = settings.createGroup("Speeds");

    private final Setting<Boolean> lockYaw = sgGeneral.add(new BoolSetting.Builder()
            .name("lock-boat-yaw")
            .description("Locks boat yaw to the direction you're facing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> verticalControl = sgGeneral.add(new BoolSetting.Builder()
            .name("vertical-control")
            .description("Whether or not space/ctrl can be used to move vertically.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> adjustHorizontalSpeed = sgGeneral.add(new BoolSetting.Builder()
            .name("adjust-horizontal-speed")
            .description("Whether or not horizontal speed is modified.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> fall = sgGeneral.add(new BoolSetting.Builder()
            .name("fall")
            .description("Toggles vertical glide.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> horizontalSpeed = sgSpeeds.add(new DoubleSetting.Builder()
            .name("horizontal-speed")
            .description("Horizontal speed in blocks per second.")
            .defaultValue(10)
            .min(0)
            .sliderMax(50)
            .build()
    );

    private final Setting<Double> verticalSpeed = sgSpeeds.add(new DoubleSetting.Builder()
            .name("vertical-speed")
            .description("Vertical speed in blocks per second.")
            .defaultValue(5)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Double> fallSpeed = sgSpeeds.add(new DoubleSetting.Builder()
            .name("fall-speed")
            .description("How fast you fall in blocks per second.")
            .defaultValue(0.625)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private BoatEntity boat = null;

    public BoatPhase() {
        super(Categories.Movement, "Boat Phase", "Phase through blocks using a boat.");
    }

    @Override
    public void onActivate() {
        boat = null;
        if (Modules.get().isActive(BoatGlitch.class)) Modules.get().get(BoatGlitch.class).toggle();
    }

    @Override
    public void onDeactivate() {
        if (boat != null) {
            boat.noClip = false;
        }
    }

    @EventHandler
    private void onBoatMove(BoatMoveEvent event) {
        if (mc.player.getVehicle() != null && mc.player.getVehicle().getType().equals(EntityType.BOAT)) {
            if (boat != mc.player.getVehicle()) {
                if (boat != null) {
                    boat.noClip = false;
                }
                boat = (BoatEntity) mc.player.getVehicle();
            }
        } else boat = null;

        if (boat != null) {
            boat.noClip = true;
            boat.pushSpeedReduction = 1;

            if (lockYaw.get()) {
                boat.yaw = mc.player.yaw;
            }

            Vec3d vel;

            if (adjustHorizontalSpeed.get()) {
                vel = PlayerUtils.getHorizontalVelocity(horizontalSpeed.get());
            }
            else {
                vel = boat.getVelocity();
            }

            double velX = vel.x;
            double velY = 0;
            double velZ = vel.z;

            if (verticalControl.get()) {
                if (mc.options.keyJump.isPressed()) velY += verticalSpeed.get() / 20;
                if (mc.options.keySprint.isPressed()) velY -= verticalSpeed.get() / 20;
                else if (fall.get()) velY -= fallSpeed.get() / 20;
            } else if (fall.get()) velY -= fallSpeed.get() / 20;

            ((IVec3d) boat.getVelocity()).set(velX,velY,velZ);
        }
    }
}