/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Rotations {
    private static final List<Rotation> queue = new ArrayList<>();
    private static final List<Rotation> rotations = new ArrayList<>();
    private static float preYaw, prePitch;
    public static float serverYaw, serverPitch;

    private static Rotation lastRotation;
    private static int lastRotationTimer;

    public static boolean rotating = false;

    private Rotations() {
    }

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(Rotations.class);
    }

    public static void rotate(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
        Rotation rotation = new Rotation(yaw, pitch, priority, clientSide, callback);

        int i = 0;
        for (; i < queue.size(); i++) {
            if (priority > queue.get(i).priority) break;
        }

        queue.add(i, rotation);
    }

    public static void rotate(double yaw, double pitch, int priority, Runnable callback) {
        rotate(yaw, pitch, priority, true, callback);
    }

    public static void rotate(double yaw, double pitch, Runnable callback) {
        rotate(yaw, pitch, 0, true, callback);
    }

    public static void rotate(double yaw, double pitch, int priority) {
        rotate(yaw, pitch, priority, true, null);
    }

    public static void rotate(double yaw, double pitch) {
        rotate(yaw, pitch, 0, true, null);
    }

    private static void setPreRotation() {
        preYaw = mc.player.getYRot();
        prePitch = mc.player.getXRot();
    }

    private static void setClientRotation(float yaw, float pitch) {
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }

    private static void setServerRotation(float yaw, float pitch) {
        serverYaw = yaw;
        serverPitch = pitch;
    }

    @EventHandler(priority = EventPriority.LOWEST - 700)
    private static void onTickPre(TickEvent.Pre event) {
        rotating = false;

        if (lastRotation != null) {
            if (lastRotationTimer++ >= Config.get().rotationHoldTicks.get()) {
                lastRotation = null;
                lastRotationTimer = 0;
            } else if (queue.isEmpty()) {
                queue.add(lastRotation);
            }
        }

        if (!queue.isEmpty()) {
            rotating = true;

            rotations.addAll(queue);

            // Set the server rotation early so movefix inputs compensate.
            Rotation last = queue.get(queue.size() - 1);
            setServerRotation(last.yaw, last.pitch);

            queue.clear();
        }
    }

    @EventHandler
    private static void onSendMovementPacketsPre(SendMovementPacketsEvent.Pre event) {
        if (rotations.isEmpty()) return;

        // The first rotation is sent within our normal movement packets
        Rotation first = rotations.get(0);
        setPreRotation();
        setServerRotation(first.yaw, first.pitch);
        setClientRotation(first.yaw, first.pitch);
    }

    @EventHandler
    private static void onSendMovementPacketsPost(SendMovementPacketsEvent.Post event) {
        if (rotations.isEmpty()) return;

        setClientRotation(preYaw, prePitch);

        // First rotation packet was already sent to server naturally
        Rotation first = rotations.get(0);
        if (first.clientSide) setClientRotation(first.yaw, first.pitch);
        first.runCallback();

        // All subsequent rotations require a packet sent
        for (int i = 1; i < rotations.size(); i++) {
            Rotation rotation = rotations.get(i);
            setServerRotation(rotation.yaw, rotation.pitch);
            if (rotation.clientSide) setClientRotation(rotation.yaw, rotation.pitch);
            rotation.sendPacket();
            rotation.runCallback();
        }

        setClientRotation(preYaw, prePitch);

        Rotation lastIndexRotation = rotations.get(rotations.size() - 1);
        if (lastIndexRotation != lastRotation) {
            lastRotation = lastIndexRotation;
            lastRotation.callback = null;
            lastRotationTimer = 0;
            rotating = Config.get().rotationHoldTicks.get() > 0;
        }

        rotations.clear();
    }

    // Utilities

    public static double getYaw(double x, double z) {
        return Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(z - mc.player.getZ(), x - mc.player.getX())) - 90f);
    }

    public static double getPitch(double x, double y, double z) {
        double diffX = x - mc.player.getX();
        double diffY = y - mc.player.getEyeY();
        double diffZ = z - mc.player.getZ();

        double diffXZ = Math.hypot(diffX, diffZ);

        return Mth.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)));
    }

    public static double getYaw(Vec3 pos) {
        return getYaw(pos.x(), pos.z());
    }

    public static double getPitch(Vec3 pos) {
        return getPitch(pos.x(), pos.y(), pos.z());
    }

    public static double getYaw(Entity entity) {
        return getYaw(entity.getX(), entity.getZ());
    }

    public static double getPitch(Entity entity, Target target) {
        double y = switch (target) {
            case Head -> entity.getEyeY();
            case Body -> entity.getY() + entity.getBbHeight() / 2;
            case Feet -> entity.getY();
        };
        
        return getPitch(entity.getX(), y, entity.getZ());
    }

    public static double getPitch(Entity entity) {
        return getPitch(entity, Target.Body);
    }

    public static double getYaw(BlockPos pos) {
        return getYaw(pos.getX() + 0.5, pos.getZ() + 0.5);
    }

    public static double getPitch(BlockPos pos) {
        return getPitch(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /**
    * setCorrectedPlayerInput and getInputFromRelativeYaw are derived from Lambda Client.
    * @see https://github.com/lambda-client/lambda/blob/1.21.11/src/main/kotlin/com/lambda/util/player/RotationUtils.kt
    * @author dinosaurwizard
    * 
    * Implementation of Grim movefix logic.
    **/
    
    public static boolean shouldCorrectPlayerInput() {
        return rotating && Config.get().rotationMovementCorrection.get();
    }

    public static void setCorrectedPlayerInput(ClientInput input) {
        input.keyPresses = getInputFromRelativeYaw(input.keyPresses);

        // The movement vector must be adjusted according to the new input
        int x = (input.keyPresses.left() ? 1 : 0) - (input.keyPresses.right() ? 1 : 0);
        int y = (input.keyPresses.forward() ? 1 : 0) - (input.keyPresses.backward() ? 1 : 0);
        input.moveVector = new Vec2(x, y).normalized();
    }

    private static Input getInputFromRelativeYaw(Input input) {
        int strafe = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
        int forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
        if (strafe == 0 && forward == 0) return input;

        double deltaYawRad = Math.toRadians(mc.player.getYRot() - serverYaw);
        double cos = Math.cos(deltaYawRad);
        double sin = Math.sin(deltaYawRad);
        double newStrafe = strafe * cos - forward * sin;
        double newForward = strafe * sin + forward * cos;
        double angle = Math.atan2(newStrafe, newForward);
        double sector = Math.PI / 4.0;
        double boundary = Math.PI / 8.0;

        boolean pressForward = false;
        boolean pressBackward = false;
        boolean pressLeft = false;
        boolean pressRight = false;

        if (angle > -boundary && angle <= boundary) {
            pressForward = true;
        } else if (angle > boundary && angle <= boundary + sector) {
            pressForward = true;
            pressLeft = true;
        } else if (angle > boundary + sector && angle <= boundary + 2 * sector) {
            pressLeft = true;
        } else if (angle > boundary + 2 * sector && angle <= boundary + 3 * sector) {
            pressBackward = true;
            pressLeft = true;
        } else if (angle > boundary + 3 * sector || angle <= -(boundary + 3 * sector)) {
            pressBackward = true;
        } else if (angle > -(boundary + 3 * sector) && angle <= -(boundary + 2 * sector)) {
            pressBackward = true;
            pressRight = true;
        } else if (angle > -(boundary + 2 * sector) && angle <= -(boundary + sector)) {
            pressRight = true;
        } else if (angle > -(boundary + sector) && angle <= -boundary) {
            pressForward = true;
            pressRight = true;
        }

        return new Input(pressForward, pressBackward, pressLeft, pressRight, input.jump(), input.shift(), input.sprint());
    }

    private static class Rotation {
        public float yaw, pitch;
        public int priority;
        public boolean clientSide;
        public Runnable callback;

        public Rotation(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
            this.yaw = (float) yaw;
            this.pitch = (float) pitch;
            this.priority = priority;
            this.clientSide = clientSide;
            this.callback = callback;
        }

        public void sendPacket() {
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
        }

        public void runCallback() {
            if (callback != null) callback.run();
        }
    }
}
