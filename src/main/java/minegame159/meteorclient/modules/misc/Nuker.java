/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Nuker extends Module {
    public enum Mode {
        All,
        Flatten,
        Smash
    }

    public enum SortMode {
        None,
        Closest,
        Furthest
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The way the blocks are broken.")
            .defaultValue(Mode.All)
            .build()
    );

    private final Setting<List<Block>> selectedBlocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("selected-blocks")
            .description("The certain type of blocks you want to mine.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private final Setting<Boolean> onlySelected = sgGeneral.add(new BoolSetting.Builder()
            .name("only-selected")
            .description("Only mines your selected blocks.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The break range.")
            .defaultValue(5)
            .min(0)
            .build()
    );

    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
            .name("sort-mode")
            .description("The blocks you want to mine first.")
            .defaultValue(SortMode.Closest)
            .build()
    );

    private final Setting<Boolean> noParticles = sgGeneral.add(new BoolSetting.Builder()
            .name("no-particles")
            .description("Disables all block breaking particles.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically faces the blocks being mined.")
            .defaultValue(true)
            .build()
    );

    private final Pool<BlockPos.Mutable> blockPool = new Pool<>(BlockPos.Mutable::new);
    private final List<BlockPos.Mutable> blocks = new ArrayList<>();
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private final BlockPos.Mutable lastBlockPos = new BlockPos.Mutable();
    private boolean hasLastBlockPos;

    public Nuker() {
        super(Categories.Misc, "nuker", "Breaks a large amount of specified blocks around you.");
    }

    @Override
    public void onDeactivate() {
        mc.interactionManager.cancelBlockBreaking();
        hasLastBlockPos = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (hasLastBlockPos && mc.world.getBlockState(lastBlockPos).getBlock() != Blocks.AIR) {
            mc.interactionManager.updateBlockBreakingProgress(lastBlockPos, Direction.UP);
            return;
        }

        hasLastBlockPos = false;

        // Calculate stuff
        double pX = mc.player.getX() - 0.5;
        double pY = mc.player.getY();
        double pZ = mc.player.getZ() - 0.5;

        int minX = (int) Math.floor(pX - range.get());
        int minY = (int) Math.floor(pY - range.get());
        int minZ = (int) Math.floor(pZ - range.get());

        int maxX = (int) Math.floor(pX + range.get());
        int maxY = (int) Math.floor(pY + range.get());
        int maxZ = (int) Math.floor(pZ + range.get());

        double rangeSq = Math.pow(range.get(), 2);

        // Find blocks to break
        for (int y = minY; y <= maxY; y++) {
            boolean skipThisYLevel = false;

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (Utils.squaredDistance(pX, pY, pZ, x, y, z) > rangeSq) continue;
                    blockPos.set(x, y, z);
                    BlockState state = mc.world.getBlockState(blockPos);
                    if (state.getOutlineShape(mc.world, blockPos) == VoxelShapes.empty()) continue;

                    // Flatten
                    if (mode.get() == Mode.Flatten && y < mc.player.getY()) {
                        skipThisYLevel = true;
                        break;
                    }

                    // Smash
                    if (mode.get() == Mode.Smash && state.getHardness(mc.world, blockPos) != 0) continue;

                    // Only selected
                    if (onlySelected.get() && !selectedBlocks.get().contains(state.getBlock())) continue;

                    BlockPos.Mutable pos = blockPool.get();
                    pos.set(x, y, z);
                    blocks.add(pos);
                }

                if (skipThisYLevel) break;
            }
        }

        // Sort blocks
        if (sortMode.get() != SortMode.None) {
            blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX(), value.getY(), value.getZ()) * (sortMode.get() == SortMode.Closest ? 1 : -1)));
        }

        // Break blocks
        boolean breaking = false;

        for (BlockPos.Mutable pos : blocks) {
            // Check last block
            if (!lastBlockPos.equals(pos)) {
                // Im not proud of this but it works so shut the fuck up
                try {
                    if (rotate.get()) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), -50, () -> cancelMine(pos));
                    else cancelMine(pos);
                } catch (Exception ignored) {}
            }

            // Break block
            lastBlockPos.set(pos);
            if (rotate.get()) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), -50, () -> normalMine(pos));
            else normalMine(pos);

            breaking = true;
            hasLastBlockPos = true;
            break;
        }

        if (!breaking) mc.interactionManager.cancelBlockBreaking();

        // Empty blocks list
        for (BlockPos.Mutable pos : blocks) blockPool.free(pos);
        blocks.clear();
    }

    private void normalMine(BlockPos pos) {
        if (mc.interactionManager != null && mc.player != null) {
            mc.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private void cancelMine(BlockPos pos) {
        mc.interactionManager.cancelBlockBreaking();
        mc.interactionManager.attackBlock(pos, Direction.UP);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    public boolean noParticles() {
        return isActive() && noParticles.get();
    }
}
