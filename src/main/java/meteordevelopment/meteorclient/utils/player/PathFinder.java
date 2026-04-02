/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PathFinder {
    private static final int PATH_AHEAD = 3;
    private static final int QUAD_1 = 1, QUAD_2 = 2, SOUTH = 0, NORTH = 180;
    private final ArrayList<PathBlock> path = new ArrayList<>(PATH_AHEAD);
    private Entity target;
    private PathBlock currentPathBlock;

    public PathBlock getNextPathBlock() {
        PathBlock nextBlock = new PathBlock(BlockPos.containing(getNextStraightPos()));
        if (isSolidFloor(nextBlock.blockPos) && isAirAbove(nextBlock.blockPos)) {
            return nextBlock;
        } else if (!isSolidFloor(nextBlock.blockPos) && isAirAbove(nextBlock.blockPos)) {
            int drop = getDrop(nextBlock.blockPos);
            if (getDrop(nextBlock.blockPos) < 3) {
                nextBlock = new PathBlock(new BlockPos(nextBlock.blockPos.getX(), nextBlock.blockPos.getY() - drop, nextBlock.blockPos.getZ()));
            }
        }

        return nextBlock;
    }

    public int getDrop(BlockPos pos) {
        int drop = 0;
        while (!isSolidFloor(pos) && drop < 3) {
            drop++;
            pos = new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
        }
        return drop;
    }

    public boolean isAirAbove(BlockPos blockPos) {
        if (!getBlockStateAtPos(blockPos.getX(), blockPos.getY(), blockPos.getZ()).isAir())
            return false;
        return getBlockStateAtPos(blockPos.getX(), blockPos.getY() + 1, blockPos.getZ()).isAir();
    }

    public Vec3 getNextStraightPos() {
        Vec3 nextPos = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        double multiplier = 1.0;
        while (nextPos == mc.player.position()) {
            nextPos = new Vec3((int) (mc.player.getX() + multiplier * Math.cos(Math.toRadians(mc.player.getYRot()))), (int) (mc.player.getY()), (int) (mc.player.getZ() + multiplier * Math.sin(Math.toRadians(mc.player.getYRot()))));
            multiplier += .1;
        }
        return nextPos;
    }

    public int getYawToTarget() {
        if (target == null || mc.player == null) return Integer.MAX_VALUE;
        Vec3 tPos = target.position();
        Vec3 pPos = mc.player.position();
        int yaw;
        int direction = getDirection();
        double tan = (tPos.z - pPos.z) / (tPos.x - pPos.x);
        if (direction == QUAD_1)
            yaw = (int) (Math.PI / 2 - Math.atan(tan));
        else if (direction == QUAD_2)
            yaw = (int) (-1 * Math.PI / 2 - Math.atan(tan));
        else return direction;
        return yaw;
    }

    public int getDirection() {
        if (target == null || mc.player == null) return 0;
        Vec3 targetPos = target.position();
        Vec3 playerPos = mc.player.position();
        if (targetPos.x == playerPos.x && targetPos.z > playerPos.z)
            return SOUTH;
        if (targetPos.x == playerPos.x && targetPos.z < playerPos.z)
            return NORTH;
        if (targetPos.x < playerPos.x)
            return QUAD_1;
        if (targetPos.x > playerPos.x)
            return QUAD_2;
        return 0;
    }

    public BlockState getBlockStateAtPos(BlockPos pos) {
        if (mc.level != null)
            return mc.level.getBlockState(pos);
        return null;
    }

    public BlockState getBlockStateAtPos(int x, int y, int z) {
        if (mc.level != null)
            return mc.level.getBlockState(new BlockPos(x, y, z));
        return null;
    }

    public Block getBlockAtPos(BlockPos pos) {
        if (mc.level != null)
            return mc.level.getBlockState(pos).getBlock();
        return null;
    }

    public boolean isSolidFloor(BlockPos blockPos) {
        return isAir(getBlockAtPos(blockPos));
    }

    public boolean isAir(Block block) {
        return block == Blocks.AIR;
    }

    public boolean isWater(Block block) {
        return block == Blocks.WATER;
    }

    public void lookAtDestination(PathBlock pathBlock) {
        if (mc.player != null) {
            mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(pathBlock.blockPos.getX(), pathBlock.blockPos.getY() + mc.player.getEyeHeight(), pathBlock.blockPos.getZ()));
        }
    }

    @EventHandler
    private void moveEventListener(PlayerMoveEvent event) {
        if (target != null && mc.player != null) {
            if (!PlayerUtils.isWithin(target, 3)) {
                if (currentPathBlock == null) currentPathBlock = getNextPathBlock();
                if (mc.player.position().distanceToSqr(new Vec3(currentPathBlock.blockPos.getX(), currentPathBlock.blockPos.getY(), currentPathBlock.blockPos.getZ())) < .01)
                    currentPathBlock = getNextPathBlock();
                lookAtDestination(currentPathBlock);
                if (!mc.options.keyUp.isDown())
                    mc.options.keyUp.setDown(true);
            } else {
                if (mc.options.keyUp.isDown())
                    mc.options.keyUp.setDown(false);
                path.clear();
                currentPathBlock = null;
            }
        }
    }

    public void initiate(Entity entity) {
        target = entity;
        if (target != null) currentPathBlock = getNextPathBlock();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void disable() {
        target = null;
        path.clear();
        if (mc.options.keyUp.isDown()) mc.options.keyUp.setDown(false);
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    public class PathBlock {
        public final Block block;
        public final BlockPos blockPos;
        public final BlockState blockState;
        public double yaw;

        public PathBlock(Block b, BlockPos pos, BlockState state) {
            block = b;
            blockPos = pos;
            blockState = state;
        }

        public PathBlock(Block b, BlockPos pos) {
            block = b;
            blockPos = pos;
            blockState = getBlockStateAtPos(blockPos);
        }

        public PathBlock(BlockPos pos) {
            blockPos = pos;
            block = getBlockAtPos(pos);
            blockState = getBlockStateAtPos(blockPos);
        }

    }
}
