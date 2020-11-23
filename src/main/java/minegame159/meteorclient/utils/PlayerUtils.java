/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.utils;

import minegame159.meteorclient.mixininterface.IVec3d;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PlayerUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Vec3d hitPos = new Vec3d(0, 0, 0);

    public static boolean placeBlock(BlockPos blockPos, int slot) {
        if (slot == -1) return false;

        int preSlot = mc.player.inventory.selectedSlot;
        mc.player.inventory.selectedSlot = slot;

        boolean a = placeBlock(blockPos);

        mc.player.inventory.selectedSlot = preSlot;
        return a;
    }

    public static boolean placeBlock(BlockPos blockPos) {
        // Check if current block is replaceable
        if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) return false;

        // Check if intersects entities
        if (!mc.world.canPlace(Blocks.STONE.getDefaultState(), blockPos, ShapeContext.absent())) return false;

        // Try to find a neighbour to click on to avoid air place
        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            Direction side2 = side.getOpposite();

            // Check if neighbour isn't empty
            if (mc.world.getBlockState(neighbor).isAir()) continue;

            // Calculate hit pos
            ((IVec3d) hitPos).set(neighbor.getX() + 0.5 + side2.getVector().getX() * 0.5, neighbor.getY() + 0.5 + side2.getVector().getY() * 0.5, neighbor.getZ() + 0.5 + side2.getVector().getZ() * 0.5);

            // Place block
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(hitPos, side2, neighbor, false));
            mc.player.swingHand(Hand.MAIN_HAND);

            return true;
        }

        // Air place if no neighbour was found
        ((IVec3d) hitPos).set(blockPos);

        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(hitPos, Direction.UP, blockPos, false));
        mc.player.swingHand(Hand.MAIN_HAND);

        return true;
    }
}
