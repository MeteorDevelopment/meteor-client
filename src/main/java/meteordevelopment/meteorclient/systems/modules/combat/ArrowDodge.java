/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ProjectileEntityAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.ProjectileEntitySimulator;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.util.*;

public class ArrowDodge extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMovement = settings.createGroup("Movement");

    private final Setting<MoveType> moveType = sgMovement.add(new EnumSetting.Builder<MoveType>()
        .name("move-type")
        .description("The way you are moved by this module.")
        .defaultValue(MoveType.Velocity)
        .build()
    );

    private final Setting<Double> moveSpeed = sgMovement.add(new DoubleSetting.Builder()
        .name("move-speed")
        .description("How fast should you be when dodging arrow.")
        .defaultValue(1)
        .min(0.01)
        .sliderRange(0.01, 5)
        .build()
    );

    private final Setting<Double> distanceCheck = sgMovement.add(new DoubleSetting.Builder()
        .name("distance-check")
        .description("How far should an arrow be from the player to be considered not hitting.")
        .defaultValue(1)
        .min(0.01)
        .sliderRange(0.01, 5)
        .build()
    );

    private final Setting<Boolean> accurate = sgGeneral.add(new BoolSetting.Builder()
        .name("accurate")
        .description("Whether or not to calculate more accurate.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> groundCheck = sgGeneral.add(new BoolSetting.Builder()
        .name("ground-check")
        .description("Tries to prevent you from falling to your death.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> allProjectiles = sgGeneral.add(new BoolSetting.Builder()
        .name("all-projectiles")
        .description("Dodge all projectiles, not only arrows.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreOwn = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-own")
        .description("Ignore your own projectiles.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> simulationSteps = sgGeneral.add(new IntSetting.Builder()
        .name("simulation-steps")
        .description("How many steps to simulate projectiles. Zero for no limit.")
        .defaultValue(500)
        .sliderMax(5000)
        .build()
    );

    private final List<Vec3d> possibleMoveDirections = Arrays.asList(
        new Vec3d(1, 0, 1),
        new Vec3d(0, 0, 1),
        new Vec3d(-1, 0, 1),
        new Vec3d(1, 0, 0),
        new Vec3d(-1, 0, 0),
        new Vec3d(1, 0, -1),
        new Vec3d(0, 0, -1),
        new Vec3d(-1, 0, -1)
    );

    private final ProjectileEntitySimulator simulator = new ProjectileEntitySimulator();
    private final Pool<Vector3d> vec3s = new Pool<>(Vector3d::new);
    private final List<Vector3d> points = new ArrayList<>();

    public ArrowDodge() {
        super(Categories.Combat, "arrow-dodge", "Tries to dodge arrows coming at you.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (Vector3d point : points) vec3s.free(point);
        points.clear();

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ProjectileEntity)) continue;
            if (!allProjectiles.get() && !(e instanceof ArrowEntity)) continue;
            if (ignoreOwn.get()) {
                UUID owner = ((ProjectileEntityAccessor) e).getOwnerUuid();
                if (owner != null && owner.equals(mc.player.getUuid())) continue;
            }
            if (!simulator.set(e, accurate.get(), 0.5D)) continue;
            for (int i = 0; i < (simulationSteps.get() > 0 ? simulationSteps.get() : Integer.MAX_VALUE); i++) {
                points.add(vec3s.get().set(simulator.pos));
                if (simulator.tick() != null) break;
            }
        }

        if (isValid(Vec3d.ZERO, false)) return; // no need to move

        double speed = moveSpeed.get();
        for (int i = 0; i < 500; i++) { // its not a while loop so it doesn't freeze if something is wrong
            boolean didMove = false;
            Collections.shuffle(possibleMoveDirections); //Make the direction unpredictable
            for (Vec3d direction : possibleMoveDirections) {
                Vec3d velocity = direction.multiply(speed);
                if (isValid(velocity, true)) {
                    move(velocity);
                    didMove = true;
                    break;
                }
            }
            if (didMove) break;
            speed += moveSpeed.get(); // move further
        }

    }

    private void move(Vec3d vel) {
        move(vel.x, vel.y, vel.z);
    }

    private void move(double velX, double velY, double velZ) {
        switch (moveType.get()) {
            case Velocity -> mc.player.setVelocity(velX, velY, velZ);
            case Packet -> {
                Vec3d newPos = mc.player.getPos().add(velX, velY, velZ);
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(newPos.x, newPos.y, newPos.z, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(newPos.x, newPos.y - 0.01, newPos.z, true));
            }
        }
    }

    private boolean isValid(Vec3d velocity, boolean checkGround) {
        Vec3d playerPos = mc.player.getPos().add(velocity);
        Vec3d headPos = playerPos.add(0, 1, 0);

        for (Vector3d pos : points) {
            Vec3d projectilePos = new Vec3d(pos.x, pos.y, pos.z);
            if (projectilePos.isInRange(playerPos, distanceCheck.get())) return false;
            if (projectilePos.isInRange(headPos, distanceCheck.get())) return false;
        }

        if (checkGround) {
            BlockPos blockPos = mc.player.getBlockPos().add(velocity.x, velocity.y, velocity.z);

            // check if target pos is air
            if (!mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty()) return false;
            else if (!mc.world.getBlockState(blockPos.up()).getCollisionShape(mc.world, blockPos.up()).isEmpty()) return false;

            if (groundCheck.get()) {
                // check if ground under target is solid
                return !mc.world.getBlockState(blockPos.down()).getCollisionShape(mc.world, blockPos.down()).isEmpty();
            }

        }

        return true;
    }

    public enum MoveType {
        Velocity,
        Packet
    }
}
