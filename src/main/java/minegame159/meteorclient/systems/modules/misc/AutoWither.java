/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoWither extends Module {

    private SettingGroup sgGeneral = settings.getDefaultGroup();

    private Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Whether or not to rotate while building")
            .defaultValue(true)
            .build()
    );

    // Part 1 : Build wither directly infront of player (complete)
    // Part 2 : Build wither based on where the player is looking
    public AutoWither() {
        super(Categories.World, "auto-wither", "Automatically builds withers.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        // Check for soulsand and skull in hotbar
        if (!hasEnoughMaterials()) {
            error("(default)Not enough resources in hotbar");
            toggle();
            return;
        }

        // Find direction of player
        // North, South, East, West
        Direction dir = getDirection(mc.gameRenderer.getCamera().getYaw() % 360);


        // Aligns player to center of block to avoid obstructing wither placement
        PlayerUtils.centerPlayer();


        // Check if we can place a wither
        BlockPos blockPos = mc.player.getBlockPos();
        blockPos = blockPos.offset(dir);

        if (!isValidSpawn(blockPos, dir)) {
            error("(default)Unable to spawn wither, obstructed by non air blocks");
            toggle();
            return;
        }


        // Build the wither
        info("(default)Spawning wither");
        spawnWither(blockPos, dir);
        toggle();
    }

    private boolean hasEnoughMaterials() {
        return (InvUtils.find(Items.SOUL_SAND).getCount() >= 4 || InvUtils.findInHotbar(Items.SOUL_SOIL).getCount() >= 4) &&
            InvUtils.find(Items.WITHER_SKELETON_SKULL).getCount() >= 3;
    }

    private Direction getDirection(float yaw) {
        if (yaw < 0) yaw += 360;

        if (yaw >= 315 || yaw < 45) return Direction.SOUTH;
        else if (yaw < 135) return Direction.WEST;
        else if (yaw < 225) return Direction.NORTH;
        else return Direction.EAST;
    }

    private boolean isValidSpawn(BlockPos blockPos, Direction direction) {

        // Withers are 3x3x1

        // Check if y > (255 - 3)
        // Because withers are 3 blocks tall
        if (blockPos.getY() > 252) return false;

        // Determine width from direction
        int widthX = 0;
        int widthZ = 0;

        if (direction == Direction.EAST || direction == Direction.WEST) widthZ = 1;
        if (direction == Direction.NORTH || direction == Direction.SOUTH) widthX = 1;


        // Check for non air blocks
        for (int x = blockPos.getX() - widthX; x <= blockPos.getX() + widthX; x++) {
            for (int z = blockPos.getZ() - widthZ; z <= blockPos.getZ(); z++) {
                for (int y = blockPos.getY(); y <= blockPos.getY() + 2; y++) {
                    if (mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.AIR) return false;
                }
            }
        }

        // Otherwise return true
        return true;
    }

    private void spawnWither(BlockPos blockPos, Direction direction) {

        // Soul sand/soil slot
        FindItemResult findSoulSand = InvUtils.findInHotbar(Items.SOUL_SAND);
        if (!findSoulSand.found()) InvUtils.findInHotbar(Items.SOUL_SOIL);

        // Skull slot
        FindItemResult findWitherSkull = InvUtils.findInHotbar(Items.WITHER_SKELETON_SKULL);

        BlockUtils.place(blockPos, findSoulSand, rotate.get(), -50, true);
        BlockUtils.place(blockPos.up(), findSoulSand, rotate.get(), -50, true);

        if (direction == Direction.EAST || direction == Direction.WEST) {
            BlockUtils.place(blockPos.up().north(), findSoulSand, rotate.get(), -50, true);
            BlockUtils.place(blockPos.up().south(), findSoulSand, rotate.get(), -50, true);

            BlockUtils.place(blockPos.up().up(), findWitherSkull, rotate.get(), -50, true);
            BlockUtils.place(blockPos.up().up().north(), findWitherSkull, rotate.get(), -50, true);
            BlockUtils.place(blockPos.up().up().south(), findWitherSkull, rotate.get(), -50, true);
        }
        else if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            BlockUtils.place(blockPos.up().east(), findSoulSand, rotate.get(), -50, true);
            BlockUtils.place(blockPos.up().west(), findSoulSand, rotate.get(), -50, true);

            BlockUtils.place(blockPos.up().up(), findWitherSkull, rotate.get(), -50, true);
            BlockUtils.place(blockPos.up().up().east(), findWitherSkull, rotate.get(), -50, true);
            BlockUtils.place(blockPos.up().up().west(), findWitherSkull, rotate.get(), -50, true);
        }
    }
}
