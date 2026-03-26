/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.simulator.ProjectileEntitySimulator;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        .defaultValue(true)
        .build()
    );

    public final Setting<Integer> simulationSteps = sgGeneral.add(new IntSetting.Builder()
        .name("simulation-steps")
        .description("How many steps to simulate projectiles. Zero for no limit.")
        .defaultValue(500)
        .sliderMax(5000)
        .build()
    );

    private final List<Vec3> possibleMoveDirections = Arrays.asList(
        new Vec3(1, 0, 1),
        new Vec3(0, 0, 1),
        new Vec3(-1, 0, 1),
        new Vec3(1, 0, 0),
        new Vec3(-1, 0, 0),
        new Vec3(1, 0, -1),
        new Vec3(0, 0, -1),
        new Vec3(-1, 0, -1)
    );

    private final ProjectileEntitySimulator simulator = new ProjectileEntitySimulator();
    private final Pool<Vector3d> vec3s = new Pool<>(Vector3d::new);
    private final List<Vector3d> points = new ArrayList<>();

    public ArrowDodge() {
        super(Categories.Combat, "arrow-dodge", "Tries to dodge arrows coming at you.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        vec3s.freeAll(points);
        points.clear();

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof Projectile projectile)) continue;
            if (!allProjectiles.get() && !(projectile instanceof Arrow || projectile instanceof SpectralArrow)) continue;
            if (ignoreOwn.get()) {
                Entity owner = projectile.getOwner();
                if (owner != null && owner.getUUID().equals(mc.player.getUUID())) continue;
            }

            if (!simulator.set(projectile)) continue;
            for (int i = 0; i < (simulationSteps.get() > 0 ? simulationSteps.get() : Integer.MAX_VALUE); i++) {
                points.add(vec3s.get().set(simulator.pos));
                if (simulator.tick().shouldStop) break;
            }
        }

        if (isValid(Vec3.ZERO, false)) return; // no need to move

        double speed = moveSpeed.get();
        for (int i = 0; i < 500; i++) { // it's not a while loop so it doesn't freeze if something is wrong
            boolean didMove = false;
            Collections.shuffle(possibleMoveDirections); //Make the direction unpredictable
            for (Vec3 direction : possibleMoveDirections) {
                Vec3 velocity = direction.scale(speed);
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

    private void move(Vec3 vel) {
        move(vel.x, vel.y, vel.z);
    }

    private void move(double velX, double velY, double velZ) {
        switch (moveType.get()) {
            case Velocity -> mc.player.setDeltaMovement(velX, velY, velZ);
            case Packet -> {
                Vec3 newPos = mc.player.position().add(velX, velY, velZ);
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(newPos.x, newPos.y, newPos.z, false, mc.player.horizontalCollision));
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(newPos.x, newPos.y - 0.01, newPos.z, true, mc.player.horizontalCollision));
            }
        }
    }

    private boolean isValid(Vec3 velocity, boolean checkGround) {
        Vec3 playerPos = mc.player.position().add(velocity);
        Vec3 headPos = playerPos.add(0, 1, 0);

        for (Vector3d pos : points) {
            Vec3 projectilePos = new Vec3(pos.x, pos.y, pos.z);
            if (projectilePos.closerThan(playerPos, distanceCheck.get())) return false;
            if (projectilePos.closerThan(headPos, distanceCheck.get())) return false;
        }

        if (checkGround) {
            BlockPos blockPos = mc.player.blockPosition().offset(BlockPos.containing(velocity.x, velocity.y, velocity.z));

            // check if target pos is air
            if (!mc.level.getBlockState(blockPos).getCollisionShape(mc.level, blockPos).isEmpty()) return false;
            else if (!mc.level.getBlockState(blockPos.above()).getCollisionShape(mc.level, blockPos.above()).isEmpty()) return false;

            if (groundCheck.get()) {
                // check if ground under target is solid
                return !mc.level.getBlockState(blockPos.below()).getCollisionShape(mc.level, blockPos.below()).isEmpty();
            }

        }

        return true;
    }

    public enum MoveType {
        Velocity,
        Packet
    }
}
