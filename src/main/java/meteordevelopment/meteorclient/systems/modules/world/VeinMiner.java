/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import com.google.common.collect.Sets;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VeinMiner extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Set<Vec3i> blockNeighbours = Sets.newHashSet(
        new Vec3i(1, -1, 1), new Vec3i(0, -1, 1), new Vec3i(-1, -1, 1),
        new Vec3i(1, -1, 0), new Vec3i(0, -1, 0), new Vec3i(-1, -1, 0),
        new Vec3i(1, -1, -1), new Vec3i(0, -1, -1), new Vec3i(-1, -1, -1),

        new Vec3i(1, 0, 1), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 1),
        new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0),
        new Vec3i(1, 0, -1), new Vec3i(0, 0, -1), new Vec3i(-1, 0, -1),

        new Vec3i(1, 1, 1), new Vec3i(0, 1, 1), new Vec3i(-1, 1, 1),
        new Vec3i(1, 1, 0), new Vec3i(0, 1, 0), new Vec3i(-1, 1, 0),
        new Vec3i(1, 1, -1), new Vec3i(0, 1, -1), new Vec3i(-1, 1, -1)
    );

    // General

    private final Setting<List<Block>> selectedBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Which blocks to select.")
        .defaultValue(Blocks.STONE, Blocks.DIRT, Blocks.GRASS)
        .build()
    );

    private final Setting<ListMode> mode = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Whitelist)
        .build()
    );

    private final Setting<Integer> depth = sgGeneral.add(new IntSetting.Builder()
        .name("depth")
        .description("Amount of iterations used to scan for similar blocks.")
        .defaultValue(3)
        .min(1)
        .sliderRange(1, 15)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between mining blocks.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Sends rotation packets to the server when mining.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> swingHand = sgRender.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client-side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether or not to render the block being mined.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 10))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    private final Pool<MyBlock> blockPool = new Pool<>(MyBlock::new);
    private final List<MyBlock> blocks = new ArrayList<>();
    private final List<BlockPos> foundBlockPositions = new ArrayList<>();

    private int tick = 0;

    public VeinMiner() {
        super(Categories.World, "vein-miner", "Mines all nearby blocks with this type");
    }

    @Override
    public void onDeactivate() {
        for (MyBlock block : blocks) blockPool.free(block);
        blocks.clear();
        foundBlockPositions.clear();
    }

    private boolean isMiningBlock(BlockPos pos) {
        for (MyBlock block : blocks) {
            if (block.blockPos.equals(pos)) return true;
        }

        return false;
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        BlockState state = mc.world.getBlockState(event.blockPos);

        if (state.getHardness(mc.world, event.blockPos) < 0)
            return;
        if (mode.get() == ListMode.Whitelist && !selectedBlocks.get().contains(state.getBlock()))
            return;
        if (mode.get() == ListMode.Blacklist && selectedBlocks.get().contains(state.getBlock()))
            return;

        foundBlockPositions.clear();

        if (!isMiningBlock(event.blockPos)) {
            MyBlock block = blockPool.get();
            block.set(event);
            blocks.add(block);
            mineNearbyBlocks(block.originalBlock.asItem(),event.blockPos,event.direction,depth.get());
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        blocks.removeIf(MyBlock::shouldRemove);

        if (!blocks.isEmpty()) {
            if (tick < delay.get() && !blocks.get(0).mining) {
                tick++;
                return;
            }
            tick = 0;
            blocks.get(0).mine();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            for (MyBlock block : blocks) block.render(event);
        }
    }

    private class MyBlock {
        public BlockPos blockPos;
        public Direction direction;
        public Block originalBlock;
        public boolean mining;

        public void set(StartBreakingBlockEvent event) {
            this.blockPos = event.blockPos;
            this.direction = event.direction;
            this.originalBlock = mc.world.getBlockState(blockPos).getBlock();
            this.mining = false;
        }

        public void set(BlockPos pos, Direction dir) {
            this.blockPos = pos;
            this.direction = dir;
            this.originalBlock = mc.world.getBlockState(pos).getBlock();
            this.mining = false;
        }

        public boolean shouldRemove() {
            return mc.world.getBlockState(blockPos).getBlock() != originalBlock || Utils.distance(mc.player.getX() - 0.5, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ() - 0.5, blockPos.getX() + direction.getOffsetX(), blockPos.getY() + direction.getOffsetY(), blockPos.getZ() + direction.getOffsetZ()) > mc.interactionManager.getReachDistance();
        }

        public void mine() {
            if (!mining) {
                mc.player.swingHand(Hand.MAIN_HAND);
                mining = true;
            }
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, this::updateBlockBreakingProgress);
            else updateBlockBreakingProgress();
        }

        private void updateBlockBreakingProgress() {
            BlockUtils.breakBlock(blockPos, swingHand.get());
        }

        public void render(Render3DEvent event) {
            VoxelShape shape = mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos);

            double x1 = blockPos.getX();
            double y1 = blockPos.getY();
            double z1 = blockPos.getZ();
            double x2 = blockPos.getX() + 1;
            double y2 = blockPos.getY() + 1;
            double z2 = blockPos.getZ() + 1;

            if (!shape.isEmpty()) {
                x1 = blockPos.getX() + shape.getMin(Direction.Axis.X);
                y1 = blockPos.getY() + shape.getMin(Direction.Axis.Y);
                z1 = blockPos.getZ() + shape.getMin(Direction.Axis.Z);
                x2 = blockPos.getX() + shape.getMax(Direction.Axis.X);
                y2 = blockPos.getY() + shape.getMax(Direction.Axis.Y);
                z2 = blockPos.getZ() + shape.getMax(Direction.Axis.Z);
            }

            event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    private void mineNearbyBlocks(Item item, BlockPos pos, Direction dir, int depth) {
        if (depth<=0) return;
        if (foundBlockPositions.contains(pos)) return;
        foundBlockPositions.add(pos);
        if (Utils.distance(mc.player.getX() - 0.5, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ() - 0.5, pos.getX(), pos.getY(), pos.getZ()) > mc.interactionManager.getReachDistance()) return;
        for(Vec3i neighbourOffset: blockNeighbours) {
            BlockPos neighbour = pos.add(neighbourOffset);
            if (mc.world.getBlockState(neighbour).getBlock().asItem() == item) {
                MyBlock block = blockPool.get();
                block.set(neighbour,dir);
                blocks.add(block);
                mineNearbyBlocks(item, neighbour, dir, depth-1);
            }
        }
    }

    @Override
    public String getInfoString() {
        return mode.get().toString() + " (" + selectedBlocks.get().size() + ")";
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
