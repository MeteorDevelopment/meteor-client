/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixin.ProjectileInGroundAccessor;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ArrowDodge extends Module {
    public enum MoveType {
        Client, Packet
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMovement = settings.createGroup("Movement");

    private final Setting<Integer> arrowLookahead = sgGeneral.add(new IntSetting.Builder()
        .name("arrow-lookahead")
        .description("How many steps into the future should be taken into consideration when deciding the direction")
        .defaultValue(500)
        .min(1)
        .max(750)
        .build()
    );

    private final Setting<MoveType> moveType = sgMovement.add(new EnumSetting.Builder<MoveType>()
        .name("move-type")
        .description("The way you are moved by this module")
        .defaultValue(MoveType.Client)
        .build()
    );

    private final Setting<Double> moveSpeed = sgMovement.add(new DoubleSetting.Builder()
            .name("move-speed")
            .description("How fast should you be when dodging arrow")
            .defaultValue(1)
            .min(0.01)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> groundCheck = sgGeneral.add(new BoolSetting.Builder()
        .name("ground-check")
        .description("Tries to prevent you from falling to your death.")
        .defaultValue(true)
        .build()
    );

    private final List<Vec3d> possibleMoveDirections = Arrays.asList(
        new Vec3d(1, 0, 1), new Vec3d(0, 0, 1), new Vec3d(-1, 0, 1),
        new Vec3d(1, 0, 0), new Vec3d(-1, 0, 0),
        new Vec3d(1, 0, -1), new Vec3d(0, 0, -1), new Vec3d(-1, 0, -1)
    );

    public ArrowDodge() {
        super(Categories.Combat, "arrow-dodge", "Tries to dodge arrows coming at you");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        Box playerHitbox = mc.player.getBoundingBox();
        if (playerHitbox == null) return;
        playerHitbox = playerHitbox.expand(0.6);

        Double speed = moveSpeed.get();

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ProjectileEntity)) continue;
            if (((ProjectileEntity)e).getOwner() == mc.player) continue;
            if (e instanceof PersistentProjectileEntity && ((ProjectileInGroundAccessor)e).getInGround()) continue;

            List<Box> futureArrowHitboxes = new ArrayList<>();

            for (int i = 0; i < arrowLookahead.get(); i++) {
                Vec3d nextPos = e.getPos().add(e.getVelocity().multiply(i / 5.0f));
                futureArrowHitboxes.add(new Box(
                        nextPos.subtract(e.getBoundingBox().getXLength() / 2, 0, e.getBoundingBox().getZLength() / 2),
                        nextPos.add(e.getBoundingBox().getXLength() / 2, e.getBoundingBox().getYLength(), e.getBoundingBox().getZLength() / 2)));
            }

            for (Box arrowHitbox: futureArrowHitboxes) {
                if (playerHitbox.intersects(arrowHitbox)) {
                    Collections.shuffle(possibleMoveDirections); //Make the direction unpredictable
                    boolean didMove = false;
                    for (Vec3d direction: possibleMoveDirections) {
                        Vec3d velocity = direction.multiply(speed);
                        if (isValid(velocity, futureArrowHitboxes, playerHitbox)) {
                            move(velocity);
                            didMove=true;
                            break;
                        }
                    }
                    if (!didMove) { //If didn't find a suitable position, run back
                        double yaw = Math.toRadians(e.yaw);
                        double pitch = Math.toRadians(e.pitch);
                        double velocityX = Math.sin(yaw) * Math.cos(pitch) * speed;
                        double velocityY = Math.sin(pitch) * speed;
                        double velocityZ = -Math.cos(yaw) * Math.cos(pitch) * speed;
                        move(new Vec3d(velocityX,velocityY,velocityZ));
                    }
                }
            }

        }
    }

    private void move(Vec3d vel) {
        MoveType mode = moveType.get();
        if (mode == MoveType.Client) {
            mc.player.setVelocity(vel);
        }
        else if (mode == MoveType.Packet) {
            Vec3d newPos = mc.player.getPos().add(vel);
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(newPos.x,newPos.y, newPos.z, false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(newPos.x,newPos.y - 0.01, newPos.z, true));
        }
    }

    private boolean isValid(Vec3d velocity, List<Box> futureArrowHitboxes, Box playerHitbox) {
        BlockPos blockPos = null;
        for (Box futureArrowHitbox: futureArrowHitboxes) {
            Box newPlayerPos = playerHitbox.offset(velocity);
            if (futureArrowHitbox.intersects(newPlayerPos)) {
                return false;
            }
            blockPos = mc.player.getBlockPos().add(velocity.x,velocity.y,velocity.z);
            if (mc.world.getBlockState(blockPos).getCollisionShape(mc.world,blockPos) != VoxelShapes.empty()) {
                return false;
            }
        }
        if ( groundCheck.get() && blockPos != null) {
            return mc.world.getBlockState(blockPos.down()).getCollisionShape(mc.world, blockPos.down()) != VoxelShapes.empty();
        }
        return true;
    }
}
