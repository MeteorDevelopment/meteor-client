/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.utils;

import minegame159.meteorclient.friends.FriendManager;
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

    public static PlayerEntity getPlayerTarget() {
        PlayerEntity closestTarget = null;
        if (mc.player == null || mc.world == null) return null;
        if (!mc.player.isAlive()) return null;

        for (PlayerEntity target : mc.world.getPlayers()) {
            if (target == mc.player || target.getHealth() <= 0 || !FriendManager.INSTANCE.attack(target) || !target.isAlive()) continue;

            if (closestTarget == null) {
                closestTarget = target;
                continue;
            }

            if (mc.player.distanceTo(target) < mc.player.distanceTo(closestTarget)) {
                closestTarget = target;
            }
        }
        return closestTarget;
    }

    public static Entity getTarget() {
        Entity target;

        if (getPlayerTarget() == null) return null;
        else target = getPlayerTarget();

        return target;
    }

    public static ArrayList<BlockPos> getTargetSurround(PlayerEntity player) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        boolean isAir = false;

        for (int i = 0; i < 4; ++i) {
            if (player == null) continue;
            BlockPos obbySurround = getSurround(player, surround[i]);
            if (obbySurround == null) continue;
            assert mc.world != null;
            if (mc.world.getBlockState(obbySurround) == null) continue;
            if (mc.world.getBlockState(obbySurround).getBlock() == Blocks.AIR) isAir = true;
            if (!(mc.world.getBlockState(obbySurround).getBlock() == Blocks.OBSIDIAN)) continue;
            positions.add(obbySurround);
        }

        if (isAir) return null;
        return positions;
    }


    public static BlockPos getTargetBlock() {
        BlockPos finalPos = null;
        boolean cancel = false;

        ArrayList<BlockPos> positions = getTargetSurround(getPlayerTarget());
        ArrayList<BlockPos> myPositions = getTargetSurround(mc.player);

        if (positions == null) return null;

        for (BlockPos pos : positions) {

            if (myPositions != null && !myPositions.isEmpty() && myPositions.contains(pos)) cancel = true;

            assert mc.world != null;
            if (mc.world.getBlockState(pos.down(1)).getBlock() != Blocks.OBSIDIAN && mc.world.getBlockState(pos.down(1)).getBlock() != Blocks.BEDROCK) continue;

            if (finalPos == null) {
                finalPos = pos;
                continue;
            }

            assert mc.player != null;
            if (mc.player.squaredDistanceTo(getVec(pos)) < mc.player.squaredDistanceTo(getVec(finalPos))) {
                finalPos = pos;
            }
        }

        if (!cancel) return finalPos;
        else return null;
    }

    public static BlockPos getSurround(Entity entity, BlockPos toAdd) {
        final Vec3d v = entity.getPos();
        if (toAdd == null) return new BlockPos(v.x, v.y, v.z);
        return new BlockPos(v.x, v.y, v.z).add(toAdd);
    }

    public static Vec3d getVec(BlockPos pos) {
        return new Vec3d(pos.getX(), pos.getY(), pos.getZ());
    }
}