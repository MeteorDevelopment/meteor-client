/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement;

import io.netty.util.internal.ConcurrentSet;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.player.PlayerMoveEvent;
import minegame159.meteorclient.events.entity.player.SendMovementPacketsEvent;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.mixin.PlayerPositionLookS2CPacketAccessor;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

public class PacketFly extends Module {
    private final Set<PlayerMoveC2SPacket> packets = new ConcurrentSet();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> flight = sgGeneral.add(new BoolSetting.Builder()
            .name("Flight")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> flightMode = sgGeneral.add(new IntSetting.Builder()
            .name("Flight Mode")
            .defaultValue(0)
            .sliderMin(0)
            .sliderMax(1)
            .min(0)
            .max(1)
            .build()
    );

    private final Setting<Boolean> doAntiFactor = sgGeneral.add(new BoolSetting.Builder()
            .name("Do AntiFactor")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> antiFactor = sgGeneral.add(new DoubleSetting.Builder()
            .name("AntiFactor")
            .defaultValue(2.5)
            .sliderMin(0.1)
            .sliderMax(3.0)
            .min(0.1)
            .max(3.0)
            .build()
    );

    private final Setting<Double> extraFactor = sgGeneral.add(new DoubleSetting.Builder()
            .name("ExtraFactor")
            .defaultValue(1.0)
            .sliderMin(0.1)
            .sliderMax(3.0)
            .min(0.1)
            .max(3.0)
            .build()
    );

    private final Setting<Boolean> strafeFactor = sgGeneral.add(new BoolSetting.Builder()
            .name("StrafeFactor")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> loops = sgGeneral.add(new IntSetting.Builder()
            .name("Loops")
            .defaultValue(1)
            .sliderMin(1)
            .sliderMax(10)
            .min(1)
            .max(10)
            .build()
    );

    private final Setting<Boolean> setYaw = sgGeneral.add(new BoolSetting.Builder()
            .name("Set Yaw")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> setID = sgGeneral.add(new BoolSetting.Builder()
            .name("Set ID")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> setMove = sgGeneral.add(new BoolSetting.Builder()
            .name("Set Move")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> noClip = sgGeneral.add(new BoolSetting.Builder()
            .name("NoClip")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> sendTeleport = sgGeneral.add(new BoolSetting.Builder()
            .name("Teleport")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> resetID = sgGeneral.add(new BoolSetting.Builder()
            .name("Reset ID")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> setPos = sgGeneral.add(new BoolSetting.Builder()
            .name("Set Pos")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> invalidPacket = sgGeneral.add(new BoolSetting.Builder()
            .name("Invalid Packet")
            .defaultValue(true)
            .build()
    );


    private int flightCounter = 0;
    private int teleportID = 0;
    private float yaw;
    private float pitch;

    public PacketFly() {
        super(Categories.Movement, "Packet Fly", "Fly using packets.");
    }

    @EventHandler
    public void onSendMovementPackets(SendMovementPacketsEvent.Pre event) {
        mc.player.setVelocity(0.0,0.0,0.0);
        double speed = 0.0;
        boolean checkCollisionBoxes = checkHitBoxes();
        speed = mc.player.input.jumping && (checkCollisionBoxes || !(mc.player.input.movementForward != 0.0 || mc.player.input.movementSideways != 0.0)) ? (flight.get() && !checkCollisionBoxes ? (flightMode.get() == 0 ? (resetCounter(10) ? -0.032 : 0.062) : (resetCounter(20) ? -0.032 : 0.062)) : 0.062) : (mc.player.input.sneaking ? -0.062 : (!checkCollisionBoxes ? (resetCounter(4) ? (flight.get() ? -0.04 : 0.0) : 0.0) : 0.0));
        if(doAntiFactor.get() && checkCollisionBoxes && (mc.player.input.movementForward != 0.0 || mc.player.input.movementSideways != 0.0) && speed != 0.0) {
            speed /= antiFactor.get();
        }
        double[] strafing = this.getMotion(strafeFactor.get() != false && checkCollisionBoxes ? 0.031 : 0.26);
        for (int i = 1; i < loops.get() + 1; ++i) {
            mc.player.setVelocity(strafing[0] * (double) i * extraFactor.get(), speed * (double) i, strafing[1] * (double) i * extraFactor.get());
            sendPackets(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z, sendTeleport.get());
        }
    }

    @EventHandler
    public void onMove (PlayerMoveEvent event) {
        if (setMove.get() && flightCounter != 0) {
            event.movement = new Vec3d(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);
            if (noClip.get() && checkHitBoxes()) {
                mc.player.noClip = true;
            }
        }
    }

    @EventHandler
    public void onPacketSent(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket && !packets.remove((PlayerMoveC2SPacket) event.packet)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket && !(mc.player == null || mc.world == null)) {
            BlockPos pos = new BlockPos(mc.player.getPos().x, mc.player.getPos().y, mc.player.getPos().z);
            PlayerPositionLookS2CPacket packet = (PlayerPositionLookS2CPacket) event.packet;
            if (setYaw.get()) {
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setPitch(mc.player.pitch);
                ((PlayerPositionLookS2CPacketAccessor) event.packet).setYaw(mc.player.yaw);
            }
            if (setID.get()) {
                teleportID = packet.getTeleportId();
            }
        }
    }

    private boolean checkHitBoxes() {
        return !(mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().expand(-0.0625,-0.0625,-0.0625)).count() == 0);
    }

    private boolean resetCounter(int counter) {
        if (++flightCounter >= counter) {
            flightCounter = 0;
            return true;
        }
        return false;
    }

    private double[] getMotion(double speed) {
        float moveForward = mc.player.input.movementForward;
        float moveStrafe = mc.player.input.movementSideways;
        float rotationYaw = mc.player.prevYaw + (mc.player.yaw - mc.player.prevYaw) * mc.getTickDelta();
        if (moveForward != 0.0f) {
            if (moveStrafe > 0.0f) {
                rotationYaw += (float) (moveForward > 0.0f ? -45 : 45);
            } else if (moveStrafe < 0.0f) {
                rotationYaw += (float) (moveForward > 0.0f ? 45 : -45);
            }
            moveStrafe = 0.0f;
            if (moveForward > 0.0f) {
                moveForward = 1.0f;
            } else if (moveForward < 0.0f) {
                moveForward = -1.0f;
            }
        }
        double posX = (double) moveForward * speed * -Math.sin(Math.toRadians(rotationYaw)) + (double) moveStrafe * speed * Math.cos(Math.toRadians(rotationYaw));
        double posZ = (double) moveForward * speed * Math.cos(Math.toRadians(rotationYaw)) - (double) moveStrafe * speed * -Math.sin(Math.toRadians(rotationYaw));
        return new double[]{posX,posZ};
    }

    private void sendPackets(double x, double y, double z, boolean teleport) {
        Vec3d vec = new Vec3d(x, y, z);
        Vec3d position = mc.player.getPos().add(vec);
        Vec3d outOfBoundsVec = outOfBoundsVec(vec, position);
        packetSender(new PlayerMoveC2SPacket.PositionOnly(position.x, position.y, position.z, mc.player.isOnGround()));
        if (invalidPacket.get()) {
            packetSender(new PlayerMoveC2SPacket.PositionOnly(outOfBoundsVec.x, outOfBoundsVec.y, outOfBoundsVec.z, mc.player.isOnGround()));
        }
        if (setPos.get()) {
            mc.player.setPos(position.x, position.y, position.z);
        }
        teleportPacket(position, teleport);
    }

    private void teleportPacket(Vec3d pos, boolean shouldTeleport) {
        if (shouldTeleport) {
            mc.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(++teleportID));
        }
    }

    private Vec3d outOfBoundsVec(Vec3d offset, Vec3d position) {
        return position.add(0.0, 1500.0, 0.0);
    }

    private void packetSender(PlayerMoveC2SPacket packet) {
        packets.add(packet);
        mc.player.networkHandler.sendPacket(packet);
    }

    private void clean() {
        flightCounter = 0;
        if (resetID.get()) {
            teleportID = 0;
        }
        packets.clear();
    }
}
