/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.world;

import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

public class BlockUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Vec3d hitPos = new Vec3d(0, 0, 0);
    public enum MobSpawning {
        Always,
        Potential,
        Never
    }

    public static boolean place(BlockPos blockPos, Hand hand, int slot, boolean rotate, int priority, boolean swing, boolean checkEntities, boolean swap, boolean swapBack) {
        if (slot == -1 || !canPlace(blockPos, checkEntities)) return false;

        Direction side = getPlaceSide(blockPos);
        BlockPos neighbour;
        Vec3d hitPos = rotate ? new Vec3d(0, 0, 0) : BlockUtils.hitPos;

        if (side == null) {
            side = Direction.UP;
            neighbour = blockPos;
            ((IVec3d) hitPos).set(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        } else {
            neighbour = blockPos.offset(side.getOpposite());
            // The Y is not 0.5 but 0.6 for allowing "antiAnchor" placement. This should not damage any other modules
            ((IVec3d) hitPos).set(neighbour.getX() + 0.5 + side.getOffsetX() * 0.5, neighbour.getY() + 0.6 + side.getOffsetY() * 0.5, neighbour.getZ() + 0.5 + side.getOffsetZ() * 0.5);
        }

        if (rotate) {
            Direction s = side;
            Rotations.rotate(Rotations.getYaw(hitPos), Rotations.getPitch(hitPos), priority, () -> place(slot, hitPos, hand, s, neighbour, swing, swap, swapBack));
        } else place(slot, hitPos, hand, side, neighbour, swing, swap, swapBack);

        return true;
    }

    public static boolean place(BlockPos blockPos, Hand hand, int slot, boolean rotate, int priority, boolean checkEntities) {
        return place(blockPos, hand, slot, rotate, priority, true, checkEntities, true, true);
    }

    private static void place(int slot, Vec3d hitPos, Hand hand, Direction side, BlockPos neighbour, boolean swing, boolean swap, boolean swapBack) {
        int preSlot = mc.player.inventory.selectedSlot;
        if (swap && !InvUtils.swap(slot)) return;

        boolean wasSneaking = mc.player.input.sneaking;
        mc.player.input.sneaking = false;

        mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(hitPos, side, neighbour, false));
        if (swing) mc.player.swingHand(hand);
        else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

        mc.player.input.sneaking = wasSneaking;

        if (swapBack) InvUtils.swap(preSlot);
    }

    public static boolean canPlace(BlockPos blockPos, boolean checkEntities) {
        if (blockPos == null) return false;

        // Check y level
        if (World.isOutOfBuildLimitVertically(blockPos)) return false;

        // Check if current block is replaceable
        if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) return false;

        // Check if intersects entities
        return !checkEntities || mc.world.canPlace(Blocks.STONE.getDefaultState(), blockPos, ShapeContext.absent());
    }

    public static boolean canPlace(BlockPos blockPos) {
        return canPlace(blockPos, true);
    }

    public static boolean isClickable(Block block) {
        return block instanceof CraftingTableBlock
                || block instanceof AnvilBlock
                || block instanceof AbstractButtonBlock
                || block instanceof AbstractPressurePlateBlock
                || block instanceof BlockWithEntity
                || block instanceof BedBlock
                || block instanceof FenceGateBlock
                || block instanceof DoorBlock
                || block instanceof NoteBlock
                || block instanceof TrapdoorBlock;
    }

    private static Direction getPlaceSide(BlockPos blockPos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            Direction side2 = side.getOpposite();

            BlockState state = mc.world.getBlockState(neighbor);

            // Check if neighbour isn't empty
            if (state.isAir() || isClickable(state.getBlock())) continue;

            // Check if neighbour is a fluid
            if (!state.getFluidState().isEmpty()) continue;

            return side2;
        }

        return null;
    }
    
    public static MobSpawning validSpawn(BlockPos blockPos) {
        BlockState blockState = mc.world.getBlockState(blockPos);
    
        if (blockPos.getY() == 0) return MobSpawning.Never;
        if (!(blockState.getBlock() instanceof AirBlock)) return MobSpawning.Never;
    
        if (!topSurface(mc.world.getBlockState(blockPos.down()))) {
            if (mc.world.getBlockState(blockPos.down()).getCollisionShape(mc.world, blockPos.down()) != VoxelShapes.fullCube()) return MobSpawning.Never;
            if (mc.world.getBlockState(blockPos.down()).isTranslucent(mc.world, blockPos.down())) return MobSpawning.Never;
        }
    
        if (mc.world.getLightLevel(blockPos, 0) <= 7) return MobSpawning.Potential;
        else if (mc.world.getLightLevel(LightType.BLOCK, blockPos) <= 7) return MobSpawning.Always;
    
        return MobSpawning.Never;
    }
    
    public static boolean topSurface(BlockState blockState) {
        if (blockState.getBlock() instanceof SlabBlock && blockState.get(SlabBlock.TYPE) == SlabType.TOP) return true;
        else return blockState.getBlock() instanceof StairsBlock && blockState.get(StairsBlock.HALF) == BlockHalf.TOP;
    }
}
