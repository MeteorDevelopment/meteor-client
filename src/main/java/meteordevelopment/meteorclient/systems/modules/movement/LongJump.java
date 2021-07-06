/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class LongJump extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<JumpMode> jumpMode = sgGeneral.add(new EnumSetting.Builder<JumpMode>()
        .name("mode")
        .description("The method of jumping.")
        .defaultValue(JumpMode.Vanilla)
        .build()
    );

    private final Setting<Double> vanillaBoostFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("vanilla-boost-factor")
        .description("The amount by which to boost the jump.")
        .visible(() -> jumpMode.get() == JumpMode.Vanilla)
        .defaultValue(1.261)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<Double> burstBoostFactor = sgGeneral.add(new DoubleSetting.Builder()
        .name("burst-boost-factor")
        .description("The amount by which to boost the jump.")
        .visible(() -> jumpMode.get() == JumpMode.Burst)
        .defaultValue(6)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-disable")
        .description("Automatically disabled the module after jumping.")
        .visible(() -> jumpMode.get() == JumpMode.Burst)
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onJump = sgGeneral.add(new BoolSetting.Builder()
        .name("on-jump")
        .description("Whether the player needs to jump first or not.")
        .visible(() -> jumpMode.get() == JumpMode.Burst)
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> disableOnRubberband = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-rubberband")
        .description("Disables the module when you get lagged back.")
        .defaultValue(true)
        .build()
    );

    public LongJump() {
        super(Categories.Movement, "long-jump", "Allows you to jump further than normal.");
    }

    private int stage;
    private double moveSpeed;
    private boolean jumping = false;

    @Override
    public void onActivate() {
        stage = 0;
        jumping = false;
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket && disableOnRubberband.get()) {
            info("Rubberband detected! Disabling...");
            toggle();
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        switch (jumpMode.get()) {
            case Vanilla -> {
                if (PlayerUtils.isMoving() && mc.options.keyJump.isPressed()) {
                    double dir = mc.player.getYaw() + ((mc.player.forwardSpeed < 0) ? 180 : 0);

                    if (mc.player.sidewaysSpeed > 0) {
                        dir += -90F * ((mc.player.forwardSpeed < 0) ? -0.5F : ((mc.player.forwardSpeed > 0) ? 0.5F : 1F));
                    }
                    else if (mc.player.sidewaysSpeed < 0) {
                        dir += 90F * ((mc.player.forwardSpeed < 0) ? -0.5F : ((mc.player.forwardSpeed > 0) ? 0.5F : 1F));
                    }

                    double xDir = Math.cos((dir + 90F) * Math.PI / 180);
                    double zDir = Math.sin((dir + 90F) * Math.PI / 180);

                    if (!mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0)) || mc.player.verticalCollision) {
                        ((IVec3d) event.movement).setXZ(xDir * 0.29F, zDir * 0.29F);
                    }
                    if ((event.movement.getY() == .33319999363422365)) {
                        ((IVec3d) event.movement).setXZ(xDir * vanillaBoostFactor.get(), zDir * vanillaBoostFactor.get());
                    }
                }
            }
            case Burst -> {
                double xDist = mc.player.getX() - mc.player.prevX;
                double zDist = mc.player.getZ() - mc.player.prevZ;
                double lastDist = Math.sqrt((xDist * xDist) + (zDist * zDist));

                if (stage != 0 && !mc.player.isOnGround() && autoDisable.get()) jumping = true;
                if (jumping && (mc.player.getY() - (int) mc.player.getY() < 0.01)) {
                    jumping = false;
                    toggle();
                    info("Disabling after jump.");
                }

                if (PlayerUtils.isMoving() && (!onJump.get() || mc.options.keyJump.isPressed()) && !mc.player.isInLava() && !mc.player.isTouchingWater()) {
                    if (stage == 0) moveSpeed = getMoveSpeed() * burstBoostFactor.get();
                    else if (stage == 1) {
                         ((IVec3d) event.movement).setY(0.42);
                         moveSpeed *= 2.149;
                    }
                    else if (stage == 2) {
                        final double difference = lastDist - getMoveSpeed();
                        moveSpeed = lastDist - difference;
                    }
                    else moveSpeed = lastDist - lastDist / 159;

                    setMoveSpeed(event, moveSpeed = Math.max(getMoveSpeed(), moveSpeed));
                    if (!mc.player.verticalCollision && !mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().y, 0.0)) && !mc.world.isSpaceEmpty(mc.player.getBoundingBox().offset(0.0, -0.4, 0.0))) {
                        ((IVec3d) event.movement).setY(-0.001);
                    }

                    stage++;
                }
            }
        }
    }

    private double getMoveSpeed() {
        double base = 0.2873;
        if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
            base *= 1.0 + 0.2 * (mc.player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1);
        }
        return base;
    }

    private void setMoveSpeed(PlayerMoveEvent event, double speed) {
        double forward = mc.player.forwardSpeed;
        double strafe = mc.player.sidewaysSpeed;
        float yaw = mc.player.getYaw();

        if (!PlayerUtils.isMoving()) {
            ((IVec3d) event.movement).setXZ(0, 0);
        }
        else {
            if (forward != 0) {
                if (strafe > 0) yaw += ((forward > 0) ? -45 : 45);
                else if (strafe < 0) yaw += ((forward > 0) ? 45 : -45);
            }
            strafe = 0;
            if (forward > 0) forward = 1;
            else if (forward < 0) forward = -1;
        }

        double cos = Math.cos(Math.toRadians(yaw + 90));
        double sin = Math.sin(Math.toRadians(yaw + 90));
        ((IVec3d) event.movement).setXZ((forward * speed * cos) + (strafe * speed * sin), (forward * speed * sin) + (strafe * speed * cos));
    }

    public enum JumpMode {
        Vanilla,
        Burst
    }
}
