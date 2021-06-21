/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AutoWither extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
        .name("horizontal-radius")
        .description("Horizontal radius for placement")
        .defaultValue(4)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
        .name("vertical-radius")
        .description("Vertical radius for placement")
        .defaultValue(3)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Priority> priority = sgGeneral.add(new EnumSetting.Builder<Priority>()
        .name("priority")
        .description("Priority")
        .defaultValue(Priority.Random)
        .build()
    );

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

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232, 10))
        .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232))
        .build()
    );

    private final Pool<Wither> witherPool = new Pool<>(Wither::new);
    private final ArrayList<Wither> withers = new ArrayList<>();
    private Wither wither;

    public AutoWither() {
        super(Categories.World, "auto-wither", "Automatically builds withers.");
    }

    @Override
    public void onDeactivate() {
        wither = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (wither == null) {
            // Clear pool and list
            for (Wither wither : withers) witherPool.free(wither);
            withers.clear();

            // Register
            BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos, blockState) -> {
                Direction dir = Direction.fromRotation(Rotations.getYaw(blockPos));
                if (isValidSpawn(blockPos, dir)) withers.add(witherPool.get().set(blockPos, dir));
            });
        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        if (wither == null) {
            if (withers.isEmpty()) return;

            // Sorting
            switch (priority.get()) {
                case Closest:
                    withers.sort(Comparator.comparingDouble(w -> PlayerUtils.distanceTo(w.foot)));
                case Furthest:
                    withers.sort((w1, w2) -> {
                        int sort = Double.compare(PlayerUtils.distanceTo(w1.foot), PlayerUtils.distanceTo(w2.foot));
                        if (sort == 0) return 0;
                        return sort > 0 ? -1 : 1;
                    });
                case Random:
                    Collections.shuffle(withers);
            }

            wither = withers.get(0);
        }

        // Soul sand/soil and skull slot
        FindItemResult findSoulSand = InvUtils.findInHotbar(Items.SOUL_SAND);
        if (!findSoulSand.found()) InvUtils.findInHotbar(Items.SOUL_SOIL);
        FindItemResult findWitherSkull = InvUtils.findInHotbar(Items.WITHER_SKELETON_SKULL);

        // Check for enough resources
        if (!findSoulSand.found() || !findWitherSkull.found()) {
            error("Not enough resources in hotbar");
            toggle();
            return;
        }


        // Build
        switch (wither.stage) {
            case 0:
                if (BlockUtils.place(wither.foot, findSoulSand, rotate.get(), -50)) wither.stage++;
                break;
            case 1:
                if (BlockUtils.place(wither.foot.up(), findSoulSand, rotate.get(), -50)) wither.stage++;
                break;
            case 2:
                if (BlockUtils.place(wither.foot.up().offset(wither.axis, -1), findSoulSand, rotate.get(), -50)) wither.stage++;
                break;
            case 3:
                if (BlockUtils.place(wither.foot.up().offset(wither.axis, 1), findSoulSand, rotate.get(), -50)) wither.stage++;
                break;
            case 4:
                if (BlockUtils.place(wither.foot.up().up(), findWitherSkull, rotate.get(), -50)) wither.stage++;
                break;
            case 5:
                if (BlockUtils.place(wither.foot.up().up().offset(wither.axis, -1), findWitherSkull, rotate.get(), -50)) wither.stage++;
                break;
            case 6:
                if (BlockUtils.place(wither.foot.up().up().offset(wither.axis, 1), findWitherSkull, rotate.get(), -50)) wither.stage++;
                break;
            case 7:
                // Auto turnoff
                if (turnOff.get()) {
                    wither = null;
                    toggle();
                }
                break;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (wither == null) return;

        // Body
        event.renderer.box(wither.foot, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        event.renderer.box(wither.foot.up(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        event.renderer.box(wither.foot.up().offset(wither.axis, -1), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        event.renderer.box(wither.foot.up().offset(wither.axis, 1), sideColor.get(), lineColor.get(), shapeMode.get(), 0);

        // Head
        BlockPos midHead = wither.foot.up().up();
        BlockPos leftHead = wither.foot.up().up().offset(wither.axis, -1);
        BlockPos rightHead = wither.foot.up().up().offset(wither.axis, 1);

        event.renderer.box((double) midHead.getX() + 0.2, (double) midHead.getX(), (double) midHead.getX() + 0.2,
                            (double) midHead.getX() + 0.8, (double) midHead.getX() + 0.7, (double) midHead.getX() + 0.8,
            sideColor.get(), lineColor.get(), shapeMode.get(), 0);

        event.renderer.box((double) leftHead.getX() + 0.2, (double) leftHead.getX(), (double) leftHead.getX() + 0.2,
                            (double) leftHead.getX() + 0.8, (double) leftHead.getX() + 0.7, (double) leftHead.getX() + 0.8,
            sideColor.get(), lineColor.get(), shapeMode.get(), 0);

        event.renderer.box((double) rightHead.getX() + 0.2, (double) rightHead.getX(), (double) rightHead.getX() + 0.2,
                            (double) rightHead.getX() + 0.8, (double) rightHead.getX() + 0.7, (double) rightHead.getX() + 0.8,
            sideColor.get(), lineColor.get(), shapeMode.get(), 0);
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


        // Check for non air blocks and entities
        BlockPos.Mutable bp = new BlockPos.Mutable();
        for (int x = blockPos.getX() - widthX; x <= blockPos.getX() + widthX; x++) {
            for (int z = blockPos.getZ() - widthZ; z <= blockPos.getZ(); z++) {
                for (int y = blockPos.getY(); y <= blockPos.getY() + 2; y++) {
                    bp.set(x, y, z);
                    if (!mc.world.getBlockState(bp).getMaterial().isReplaceable()) return false;
                    if (!mc.world.canPlace(Blocks.STONE.getDefaultState(), bp, ShapeContext.absent())) return false;
                }
            }
        }

        return true;
    }

    public enum Priority {
        Closest,
        Furthest,
        Random
    }

    private static class Wither {
        public int stage;
        // 0 = foot
        // 1 = mid body
        // 2 = left arm
        // 3 = right arm
        // 4 = mid head
        // 5 = left head
        // 6 = right head
        // 7 = end
        public BlockPos.Mutable foot = new BlockPos.Mutable();
        public Direction facing;
        public Direction.Axis axis;

        public Wither set(BlockPos pos, Direction dir) {
            this.stage = 0;
            this.foot.set(pos);
            this.facing = dir;
            this.axis = dir.getAxis();

            return this;
        }
    }
}
