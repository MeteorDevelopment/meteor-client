/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.player;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.Target;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationUtils {
    public static MinecraftClient mc = MinecraftClient.getInstance();

    public static float serverYaw;
    public static float serverPitch;
    public static int rotationTimer;

    public static void packetRotate(Entity entity) {
        packetRotate(entity, Target.Body);
    }

        public static void packetRotate(Entity entity, Target target) {
        switch (target) {
            case Head:
                packetRotate(new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ()));
                break;
            case Body:
                packetRotate(new Vec3d(entity.getX(), (entity.getY() + entity.getHeight() / 2), entity.getZ()));
                break;
            case Feet:
                packetRotate(getNeededYaw(entity.getPos()), getNeededPitch(entity.getPos()));
                break;
        }
    }

    public static void packetRotate(BlockPos blockPos) {
        packetRotate(getNeededYaw(Utils.vec3d(blockPos)), getNeededPitch(Utils.vec3d(blockPos)));
    }

    public static void packetRotate(Vec3d vec) {
        packetRotate(getNeededYaw(vec), getNeededPitch(vec));
    }

    public static void packetRotate(float yaw, float pitch) {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(yaw, pitch, mc.player.isOnGround()));
        setCamRotation(yaw, pitch);
    }

    public static void setCamRotation(double yaw, double pitch) {
        serverYaw = (float) yaw;
        serverPitch = (float) pitch;
        rotationTimer = 0;
    }

    public static void clientRotate(BlockPos blockPos) {
        mc.player.yaw = getNeededYaw(Utils.vec3d(blockPos));
        mc.player.pitch = getNeededPitch(Utils.vec3d(blockPos));
    }

    public static void clientRotate(Vec3d vec) {
        mc.player.yaw = getNeededYaw(vec);
        mc.player.pitch = getNeededPitch(vec);
    }

    public static float getNeededYaw(Vec3d vec) {
        return mc.player.yaw + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(vec.z - mc.player.getZ(), vec.x - mc.player.getX())) - 90f - mc.player.yaw);
    }

    public static float getNeededPitch(Vec3d vec) {
        double diffX = vec.x - mc.player.getX();
        double diffY = vec.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = vec.z - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.pitch + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.pitch);
    }

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(RotationUtils.class);
    }

    @EventHandler
    private static void onTick(TickEvent.Pre event) {
        rotationTimer++;
    }
}
