/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.entity;

import minegame159.meteorclient.utils.player.PlayerUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static minegame159.meteorclient.utils.Utils.mc;

public class EntityUtils {
    public static boolean isAttackable(EntityType<?> type) {
        return type != EntityType.AREA_EFFECT_CLOUD && type != EntityType.ARROW && type != EntityType.FALLING_BLOCK && type != EntityType.FIREWORK_ROCKET && type != EntityType.ITEM && type != EntityType.LLAMA_SPIT && type != EntityType.SPECTRAL_ARROW && type != EntityType.ENDER_PEARL && type != EntityType.EXPERIENCE_BOTTLE && type != EntityType.POTION && type != EntityType.TRIDENT && type != EntityType.LIGHTNING_BOLT && type != EntityType.FISHING_BOBBER && type != EntityType.EXPERIENCE_ORB && type != EntityType.EGG;
    }

    public static float getTotalHealth(PlayerEntity target) {
        return target.getHealth() + target.getAbsorptionAmount();
    }

    public static int getPing(PlayerEntity player) {
        if (mc.getNetworkHandler() == null) return 0;

        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (playerListEntry == null) return 0;
        return playerListEntry.getLatency();
    }

    public static GameMode getGameMode(PlayerEntity player) {
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (playerListEntry == null) return null;
        return playerListEntry.getGameMode();
    }

    public static boolean isAboveWater(Entity entity) {
        BlockPos.Mutable blockPos = entity.getBlockPos().mutableCopy();

        for (int i = 0; i < 64; i++) {
            BlockState state = mc.world.getBlockState(blockPos);

            if (state.getMaterial().blocksMovement()) break;

            Fluid fluid = state.getFluidState().getFluid();
            if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
                return true;
            }

            blockPos.move(0, -1, 0);
        }

        return false;
    }

    public static boolean isInRenderDistance(Entity entity) {
        if (entity == null) return false;
        return isInRenderDistance(entity.getX(), entity.getZ());
    }

    public static boolean isInRenderDistance(BlockEntity entity) {
        if (entity == null) return false;
        return isInRenderDistance(entity.getPos().getX(), entity.getPos().getZ());
    }

    public static boolean isInRenderDistance(BlockPos pos) {
        if (pos == null) return false;
        return isInRenderDistance(pos.getX(), pos.getZ());
    }

    public static boolean isInRenderDistance(double posX, double posZ) {
        double x = Math.abs(mc.gameRenderer.getCamera().getPos().x - posX);
        double z = Math.abs(mc.gameRenderer.getCamera().getPos().z - posZ);
        double d = (mc.options.viewDistance + 1) * 16;

        return x < d && z < d;
    }

    public static List<BlockPos> getSurroundBlocks(PlayerEntity player) {
        if (player == null) return null;

        List<BlockPos> positions = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;

            BlockPos pos = player.getBlockPos().offset(direction);

            if (mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN) {
                positions.add(pos);
            }
        }

        return positions;
    }

    public static BlockPos getCityBlock(PlayerEntity player) {
        List<BlockPos> posList = getSurroundBlocks(player);
        posList.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        return posList.isEmpty() ? null : posList.get(0);
    }
}
