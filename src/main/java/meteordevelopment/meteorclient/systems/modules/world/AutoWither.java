/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

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
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoWither extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Whether or not to rotate while building")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
        .name("turn-off")
        .description("Turns off automatically after building a single wither.")
        .defaultValue(true)
        .build()
    );


    private BlockPos.Mutable bp = new BlockPos.Mutable();
    private boolean return_;


    public AutoWither() {
        super(Categories.World, "auto-wither", "Automatically builds withers.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Soul sand/soil and skull slot
        FindItemResult findSoulSand = InvUtils.findInHotbar(Items.SOUL_SAND);
        if (!findSoulSand.found()) InvUtils.findInHotbar(Items.SOUL_SOIL);

        FindItemResult findWitherSkull = InvUtils.findInHotbar(Items.WITHER_SKELETON_SKULL);

        // Check for enough resources
        if (findSoulSand.getCount() < 4 || findWitherSkull.getCount() < 3) {
            error("Not enough resources in hotbar");
            toggle();
            return;
        }

        // Valid placement check
        Direction dir = mc.player.getHorizontalFacing();
        BlockPos blockPos = mc.player.getBlockPos();
        blockPos = blockPos.offset(dir);

        if (!isValidSpawn(blockPos, dir)) return;


        // Opposite axis
        Direction.Axis oppositeAxis;
        if (dir == Direction.EAST || dir == Direction.WEST) oppositeAxis = Direction.Axis.Z;
        else oppositeAxis = Direction.Axis.X;

        return_ = false;

        // Body (soul sand)
        boolean p1 = place(blockPos, findSoulSand);
        if (return_) return;
        boolean p2 = place(blockPos.up(), findSoulSand);
        if (return_) return;
        boolean p3 = place(blockPos.up().offset(oppositeAxis, 1), findSoulSand);
        if (return_) return;
        boolean p4 = place(blockPos.up().offset(oppositeAxis, -1), findSoulSand);
        if (return_) return;

        // Head (wither skulls)
        boolean p5 = place(blockPos.up().up(), findWitherSkull);
        if (return_) return;
        boolean p6 = place(blockPos.up().up().offset(oppositeAxis, 1), findWitherSkull);
        if (return_) return;
        boolean p7 = place(blockPos.up().up().offset(oppositeAxis, -1), findWitherSkull);
        if (return_) return;


        // Auto turnoff
        if (turnOff.get() &&
            p1 && p2 && p3 && p4 && p5 && p6 && p7) {

            toggle();
        }
    }

    private boolean hasEnoughMaterials() {
        return (InvUtils.find(Items.SOUL_SAND).getCount() >= 4 || InvUtils.findInHotbar(Items.SOUL_SOIL).getCount() >= 4) &&
            InvUtils.find(Items.WITHER_SKELETON_SKULL).getCount() >= 3;
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
                    bp.set(x, y, z);
                    if (mc.world.getBlockState(bp).getBlock() != Blocks.AIR) return false;
                    if (!mc.world.canPlace(Blocks.STONE.getDefaultState(), bp, ShapeContext.absent())) return false;
                }
            }
        }

        return true;
    }

    private boolean place(BlockPos blockPos, FindItemResult findItemResult) {
        PlayerUtils.centerPlayer();

        bp.set(blockPos);
        BlockState blockState = mc.world.getBlockState(blockPos);

        if (!blockState.getMaterial().isReplaceable()) return true;

        if (BlockUtils.place(blockPos, findItemResult, rotate.get(), -50, true)) {
            return_ = true;
        }

        return false;
    }
}
