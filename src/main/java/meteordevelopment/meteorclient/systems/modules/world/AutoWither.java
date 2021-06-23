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

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Which mode to use for placing withers.")
        .defaultValue(Mode.Linear)
        .build()
    );

    private final Setting<Integer> horizontalRange = sgGeneral.add(new IntSetting.Builder()
        .name("horizontal-range")
        .description("Horizontal radius for placement")
        .defaultValue(4)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> verticalRange = sgGeneral.add(new IntSetting.Builder()
        .name("vertical-range")
        .description("Vertical radius for placement")
        .defaultValue(3)
        .min(0)
        .sliderMax(6)
        .visible(() -> mode.get() == Mode.Area)
        .build()
    );

    private final Setting<Integer> minimumDistance = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-distance")
        .description("Minimum distance from the player to build the wither")
        .defaultValue(1)
        .min(1)
        .sliderMax(3)
        .build()
    );

    private final Setting<Priority> priority = sgGeneral.add(new EnumSetting.Builder<Priority>()
        .name("priority")
        .description("Priority")
        .defaultValue(Priority.Random)
        .build()
    );

    private final Setting<Integer> witherDelay = sgGeneral.add(new IntSetting.Builder()
        .name("wither-delay")
        .description("Delay in ticks between wither placements")
        .defaultValue(1)
        .min(1)
        .sliderMax(10)
        .build()
    );

    private final Setting<Integer> blockDelay = sgGeneral.add(new IntSetting.Builder()
        .name("block-delay")
        .description("Delay in ticks between block placements")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
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
        .description("The side color wither rendering.")
        .defaultValue(new SettingColor(127, 0, 255, 10))
        .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color wither rendering.")
        .defaultValue(new SettingColor(127, 0, 255))
        .build()
    );

    private final Pool<Wither> witherPool = new Pool<>(Wither::new);
    private final ArrayList<Wither> withers = new ArrayList<>();
    private Wither wither;
    private final BlockPos.Mutable bp = new BlockPos.Mutable();

    private Direction playerFacing;

    private int witherTicksWaited, blockTicksWaited;

    public AutoWither() {
        super(Categories.World, "auto-wither", "Automatically builds withers.");
    }

    @Override
    public void onActivate() {
        if (mode.get() == Mode.Linear) {
            playerFacing = mc.player.getHorizontalFacing();
        }
    }

    @Override
    public void onDeactivate() {
        wither = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (wither == null) {
            // Delay
            if (witherTicksWaited < witherDelay.get() - 1) {
                return;
            }


            // Check for enough resources
            FindItemResult findSoulSand = InvUtils.findInHotbar(Items.SOUL_SAND);
            if (!findSoulSand.found()) InvUtils.findInHotbar(Items.SOUL_SOIL);
            FindItemResult findWitherSkull = InvUtils.findInHotbar(Items.WITHER_SKELETON_SKULL);

            if (findSoulSand.getCount() < 4 || findWitherSkull.getCount() < 3) {
                error("Not enough resources in hotbar");
                toggle();
                return;
            }


            // Clear pool and list
            for (Wither wither : withers) witherPool.free(wither);
            withers.clear();

            // Register
            if (mode.get() == Mode.Area) {
                // Area
                BlockIterator.register(horizontalRange.get(), verticalRange.get(), (blockPos, blockState) -> {
                    if (PlayerUtils.distanceTo(blockPos) < minimumDistance.get()) return;

                    // Invert axis
                    Direction.Axis axis = Direction.fromRotation(Rotations.getYaw(blockPos)).getAxis();
                    axis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;

                    // Valid spawn check
                    if (isValidSpawn(blockPos, axis)) withers.add(witherPool.get().set(blockPos, axis));
                });
            } else {
                // Linear
                while (PlayerUtils.distanceTo(bp) < horizontalRange.get()) {
                    // Invert axis
                    Direction.Axis axis = playerFacing.getAxis();
                    axis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;

                    // Valid spawn check
                    if (minimumDistance.get() < PlayerUtils.distanceTo(bp) && isValidSpawn(bp, axis)) withers.add(witherPool.get().set(bp, axis));

                    bp.move(playerFacing);
                }
            }


        }
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        // Get next wither
        if (wither == null) {
            // Delay
            if (witherTicksWaited < witherDelay.get() - 1) {
                witherTicksWaited++;
                return;
            }


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
        if (blockDelay.get() == 0) {
            // All in 1 tick

            // Body
            BlockUtils.place(wither.foot, findSoulSand, rotate.get(), -50);
            BlockUtils.place(wither.foot.up(), findSoulSand, rotate.get(), -50);
            BlockUtils.place(wither.foot.up().offset(wither.axis, -1), findSoulSand, rotate.get(), -50);
            BlockUtils.place(wither.foot.up().offset(wither.axis, 1), findSoulSand, rotate.get(), -50);

            // Head
            BlockUtils.place(wither.foot.up().up(), findWitherSkull, rotate.get(), -50);
            BlockUtils.place(wither.foot.up().up().offset(wither.axis, -1), findWitherSkull, rotate.get(), -50);
            BlockUtils.place(wither.foot.up().up().offset(wither.axis, 1), findWitherSkull, rotate.get(), -50);


            // Auto turnoff
            if (turnOff.get()) {
                toggle();
            }

            // Reset wither
            wither = null;

        } else {
            // Delay
            if (blockTicksWaited < blockDelay.get() - 1) {
                blockTicksWaited++;
                return;
            }

            switch (wither.stage) {
                // Body
                case 0:
                    if (BlockUtils.place(wither.foot, findSoulSand, rotate.get(), -50)) wither.stage++;
                    break;
                case 1:
                    if (BlockUtils.place(wither.foot.up(), findSoulSand, rotate.get(), -50)) wither.stage++;
                    break;
                case 2:
                    if (BlockUtils.place(wither.foot.up().offset(wither.axis, 1), findSoulSand, rotate.get(), -50)) wither.stage++;
                    break;
                case 3:
                    if (BlockUtils.place(wither.foot.up().offset(wither.axis, -1), findSoulSand, rotate.get(), -50)) wither.stage++;
                    break;

                // Heads
                case 4:
                    if (BlockUtils.place(wither.foot.up().up(), findWitherSkull, rotate.get(), -50)) wither.stage++;
                    break;
                case 5:
                    if (BlockUtils.place(wither.foot.up().up().offset(wither.axis, 1), findWitherSkull, rotate.get(), -50)) wither.stage++;
                    break;
                case 6:
                    if (BlockUtils.place(wither.foot.up().up().offset(wither.axis, -1), findWitherSkull, rotate.get(), -50)) {

                        // Auto turnoff
                        if (turnOff.get()) {
                            toggle();
                        }

                        // Reset wither
                        wither = null;
                    }
                    break;
            }
        }


        witherTicksWaited = 0;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (wither == null) return;

        // Body
        if (wither.stage <= 0) event.renderer.box(wither.foot, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if (wither.stage <= 1) event.renderer.box(wither.foot.up(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if (wither.stage <= 2) event.renderer.box(wither.foot.up().offset(wither.axis, -1), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        if (wither.stage <= 3) event.renderer.box(wither.foot.up().offset(wither.axis, 1), sideColor.get(), lineColor.get(), shapeMode.get(), 0);

        // Heads
        if (wither.stage <= 4) {
            BlockPos midHead = wither.foot.up().up();
            event.renderer.box((double) midHead.getX() + 0.2, midHead.getY(), (double) midHead.getZ() + 0.2,
                                (double) midHead.getX() + 0.8, (double) midHead.getY() + 0.6, (double) midHead.getZ() + 0.8,
                    sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }

        if (wither.stage <= 5) {
            BlockPos leftHead = wither.foot.up().up().offset(wither.axis, 1);
            event.renderer.box((double) leftHead.getX() + 0.2, leftHead.getY(), (double) leftHead.getZ() + 0.2,
                                (double) leftHead.getX() + 0.8, (double) leftHead.getY() + 0.6, (double) leftHead.getZ() + 0.8,
                    sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }

        if (wither.stage <= 6) {
            BlockPos rightHead = wither.foot.up().up().offset(wither.axis, -1);
            event.renderer.box((double) rightHead.getX() + 0.2, rightHead.getY(), (double) rightHead.getZ() + 0.2,
                                (double) rightHead.getX() + 0.8, (double) rightHead.getY() + 0.6, (double) rightHead.getZ() + 0.8,
                    sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    private boolean isValidSpawn(BlockPos blockPos, Direction.Axis axis) {
        // Withers are 3x3x1

        // Check if y > (255 - 3)
        // Because withers are 3 blocks tall
        if (blockPos.getY() > 252) return false;


        // Determine width from axis
        int widthX = 0;
        int widthZ = 0;

        if (axis == Direction.Axis.X) {
            widthX = 1;

            // Air place check
            if (mc.world.getBlockState(blockPos.south()).getMaterial().isReplaceable() &&
                mc.world.getBlockState(blockPos.north()).getMaterial().isReplaceable() &&
                mc.world.getBlockState(blockPos.down()).getMaterial().isReplaceable()) return false;

        }
        if (axis == Direction.Axis.Z) {
            widthZ = 1;

            // Air place check
            if (mc.world.getBlockState(blockPos.east()).getMaterial().isReplaceable() &&
                mc.world.getBlockState(blockPos.west()).getMaterial().isReplaceable() &&
                mc.world.getBlockState(blockPos.down()).getMaterial().isReplaceable()) return false;
        }

        // Check for obstruction
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

    private static class Wither {
        public int stage;
        // 0 = foot
        // 1 = mid body
        // 2 = left arm
        // 3 = right arm
        // 4 = mid head
        // 5 = left head
        // 6 = right head
        public BlockPos.Mutable foot = new BlockPos.Mutable();
        public Direction.Axis axis;

        public Wither set(BlockPos pos, Direction.Axis axis) {
            this.stage = 0;
            this.foot.set(pos);
            this.axis = axis;

            return this;
        }
    }

    public enum Priority {
        Closest,
        Furthest,
        Random
    }

    public enum Mode {
        Linear,
        Area
    }
}
