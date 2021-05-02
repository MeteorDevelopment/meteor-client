/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.player;

import minegame159.meteorclient.mixin.AbstractBlockAccessor;
import minegame159.meteorclient.systems.friends.Friends;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import minegame159.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class CityUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final BlockPos[] surround = {
            new BlockPos(0, 0, -1),
            new BlockPos(1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(-1, 0, 0)
    };

    public static PlayerEntity getPlayerTarget(double range) {
        if (mc.player.isDead()) return null;

        PlayerEntity closestTarget = null;

        for (PlayerEntity target : mc.world.getPlayers()) {
            if (target == mc.player
                    || target.isDead()
                    || !Friends.get().attack(target)
                    || mc.player.distanceTo(target) > range
            ) continue;

            if (closestTarget == null) {
                closestTarget = target;
                continue;
            }

            if (mc.player.distanceTo(target) < mc.player.distanceTo(closestTarget)) {
                closestTarget = target;
            }
        }

        if (closestTarget == null) {
            for (FakePlayerEntity target : FakePlayerManager.getPlayers()) {
                if (target.isDead() || !Friends.get().attack(target) || mc.player.distanceTo(target) > range) continue;

                if (closestTarget == null) {
                    closestTarget = target;
                    continue;
                }

                if (mc.player.distanceTo(target) < mc.player.distanceTo(closestTarget)) {
                    closestTarget = target;
                }
            }
        }

        return closestTarget;
    }

    public static BlockPos getTargetBlock(PlayerEntity target) {
        BlockPos finalPos = null;

        ArrayList<BlockPos> positions = getTargetSurround(target);
        ArrayList<BlockPos> myPositions = getTargetSurround(mc.player);

        if (positions == null) return null;

        for (BlockPos pos : positions) {

            if (myPositions != null && !myPositions.isEmpty() && myPositions.contains(pos)) continue;

            if (finalPos == null) {
                finalPos = pos;
                continue;
            }

            if (mc.player.squaredDistanceTo(Utils.vec3d(pos)) < mc.player.squaredDistanceTo(Utils.vec3d(finalPos))) {
                finalPos = pos;
            }
        }

        return finalPos;
    }

    private static ArrayList<BlockPos> getTargetSurround(PlayerEntity player) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        boolean isAir = false;

        for (int i = 0; i < 4; ++i) {
            if (player == null) continue;
            BlockPos obbySurround = getSurround(player, surround[i]);
            if (obbySurround == null) continue;
            assert mc.world != null;
            if (mc.world.getBlockState(obbySurround) == null) continue;
            if (!((AbstractBlockAccessor) mc.world.getBlockState(obbySurround).getBlock()).isCollidable()) isAir = true;
            if (!(mc.world.getBlockState(obbySurround).getBlock() == Blocks.OBSIDIAN)) continue;
            positions.add(obbySurround);
        }

        if (isAir) return null;
        return positions;
    }

    public static BlockPos getSurround(Entity entity, BlockPos toAdd) {
        final Vec3d v = entity.getPos();
        if (toAdd == null) return new BlockPos(v.x, v.y, v.z);
        return new BlockPos(v.x, v.y, v.z).add(toAdd);
    }
}