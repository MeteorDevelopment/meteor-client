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
import minegame159.meteorclient.utils.player.PlayerUtils;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

public class PacketFly extends Module {
    private final Set<PlayerMoveC2SPacket> packets = new ConcurrentSet();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMovement = settings.createGroup("Movement");
    private final SettingGroup sgClient = settings.createGroup("Client");
    private final SettingGroup sgBypass = settings.createGroup("Bypass");

    private final Setting<Double> horizontalSpeed = sgMovement.add(new DoubleSetting.Builder()
            .name("Horizontal Speed")
            .description("Horizontal speed in blocks per second.")
            .defaultValue(5.2)
            .min(0.0)
            .max(20.0)
            .sliderMin(0.0)
            .sliderMax(20.0)
            .build()
    );

    private final Setting<Double> verticalSpeed = sgMovement.add(new DoubleSetting.Builder()
            .name("Vertical Speed")
            .description("Vertical speed in blocks per second.")
            .defaultValue(1.24)
            .min(0.0)
            .max(5.0)
            .sliderMin(0.0)
            .sliderMax(20.0)
            .build()
    );

    private final Setting<Boolean> sendTeleport = sgMovement.add(new BoolSetting.Builder()
            .name("Teleport")
            .description("Sends teleport packets.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> setYaw = sgClient.add(new BoolSetting.Builder()
            .name("Set Yaw")
            .description("Sets yaw client side.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> setMove = sgClient.add(new BoolSetting.Builder()
            .name("Set Move")
            .description("Sets movement client side.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> setPos = sgClient.add(new BoolSetting.Builder()
            .name("Set Pos")
            .description("Sets position client side.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> setID = sgClient.add(new BoolSetting.Builder()
            .name("Set ID")
            .description("Updates teleport id when a position packet is received.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> noClip = sgClient.add(new BoolSetting.Builder()
            .name("NoClip")
            .description("Makes the client ignore walls.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> antiKick = sgBypass.add(new BoolSetting.Builder()
            .name("Anti Kick")
            .description("Moves down occasionally to prevent kicks.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> downDelay = sgBypass.add(new IntSetting.Builder()
            .name("Down Delay")
            .description("How often you move down when not flying upwards. (ticks)")
            .defaultValue(4)
            .sliderMin(1)
            .sliderMax(30)
            .min(1)
            .max(30)
            .build()
    );

    private final Setting<Integer> downDelayFlying = sgBypass.add(new IntSetting.Builder()
            .name("Down Delay (Flying)")
            .description("How often you move down when flying upwards. (ticks)")
            .defaultValue(10)
            .sliderMin(1)
            .sliderMax(30)
            .min(1)
            .max(30)
            .build()
    );

    private final Setting<Boolean> invalidPacket = sgBypass.add(new BoolSetting.Builder()
            .name("Invalid Packet")
            .description("Sends invalid movement packets.")
            .defaultValue(true)
            .build()
    );

    private int flightCounter = 0;
    private int teleportID = 0;

    public PacketFly() {
        super(Categories.Movement, "Packet Fly", "Fly using packets.");
    }

    @EventHandler
    public void onSendMovementPackets(SendMovementPacketsEvent.Pre event) {
        mc.player.setVelocity(0.0,0.0,0.0);
        double speed = 0.0;
        boolean checkCollisionBoxes = checkHitBoxes();

        speed = mc.player.input.jumping && (checkCollisionBoxes || !(mc.player.input.movementForward != 0.0 || mc.player.input.movementSideways != 0.0)) ? (antiKick.get() && !checkCollisionBoxes ? (resetCounter(downDelayFlying.get()) ? -0.032 : verticalSpeed.get()/20) : verticalSpeed.get()/20) : (mc.player.input.sneaking ? verticalSpeed.get()/-20 : (!checkCollisionBoxes ? (resetCounter(downDelay.get()) ? (antiKick.get() ? -0.04 : 0.0) : 0.0) : 0.0));

        Vec3d horizontal = PlayerUtils.getHorizontalVelocity(horizontalSpeed.get());

        mc.player.setVelocity(horizontal.x, speed, horizontal.z);
        sendPackets(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z, sendTeleport.get());
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
}
