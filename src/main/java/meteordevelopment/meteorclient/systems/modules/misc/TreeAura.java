/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SaplingBlock;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class TreeAura extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> rotation = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("rotate for block interactions").defaultValue(false).build());
    private final Setting<Integer> plantDelay = sgGeneral.add(new IntSetting.Builder().name("plant-delay").description("delay between planting trees").defaultValue(6).min(0).sliderMax(25).build());
    private final Setting<Integer> bonemealDelay = sgGeneral.add(new IntSetting.Builder().name("bonemeal-delay").description("delay between placing bonemeal on trees").defaultValue(3).min(0).sliderMax(25).build());
    private final Setting<Integer> rRange = sgGeneral.add(new IntSetting.Builder().name("radius").description("how far you can place horizontally").defaultValue(4).min(1).sliderMax(5).build());
    private final Setting<Integer> yRange = sgGeneral.add(new IntSetting.Builder().name("y-range").description("how far you can place vertically").defaultValue(3).min(1).sliderMax(5).build());
    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>().name("sort-mode").description("how to sort nearby trees/placements.").defaultValue(SortMode.Farthest).build());

    private int bonemealTimer, plantTimer;


    public TreeAura() { // CopeTypes
        super(Categories.Misc, "tree-aura", "Plants trees around you");
    }

    @Override
    public void onActivate() {
        bonemealTimer = 0;
        plantTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        plantTimer--;
        bonemealTimer--;

        if (plantTimer <= 0) {
            BlockPos plantPos = findPlantLocation();
            if (plantPos == null) return;
            doPlant(plantPos);
            plantTimer = plantDelay.get();
        }

        if (bonemealTimer <= 0) {
            BlockPos p = findPlantedSapling();
            if (p == null) return;
            doBonemeal(p);
            bonemealTimer = bonemealDelay.get();
        }
    }


    private FindItemResult findBonemeal() {
        return InvUtils.findInHotbar(Items.BONE_MEAL);
    }

    private FindItemResult findSapling() {
        return InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()) instanceof SaplingBlock);
    }

    private boolean isSapling(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() instanceof SaplingBlock;
    }

    private void doPlant(BlockPos plantPos) {
        FindItemResult sapling = findSapling();
        if (!sapling.found()) {
            error("No saplings in hotbar");
            toggle();
            return;
        }
        InvUtils.swap(sapling.slot(), false);
        if (rotation.get())
            Rotations.rotate(Rotations.getYaw(plantPos), Rotations.getPitch(plantPos), () -> mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(plantPos), Direction.UP, plantPos, false))));
        else
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(plantPos), Direction.UP, plantPos, false)));
    }

    private void doBonemeal(BlockPos sapling) {
        FindItemResult bonemeal = findBonemeal();
        if (!bonemeal.found()) {
            error("No bonemeal in hotbar");
            toggle();
            return;
        }
        InvUtils.swap(bonemeal.slot(), false);
        if (rotation.get())
            Rotations.rotate(Rotations.getYaw(sapling), Rotations.getPitch(sapling), () -> mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(sapling), Direction.UP, sapling, false))));
        else
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(sapling), Direction.UP, sapling, false)));
    }

    private boolean canPlant(BlockPos pos) {
        Block b = mc.world.getBlockState(pos).getBlock();
        if (b.equals(Blocks.GRASS) || b.equals(Blocks.GRASS_BLOCK) || b.equals(Blocks.DIRT) || b.equals(Blocks.COARSE_DIRT)) {
            final AtomicBoolean plant = new AtomicBoolean(true);
            IntStream.rangeClosed(1, 5).forEach(i -> {
                // Check above
                BlockPos check = pos.up(i);
                if (!mc.world.getBlockState(check).getBlock().equals(Blocks.AIR)) {
                    plant.set(false);
                    return;
                }
                // Check around
                for (CardinalDirection dir : CardinalDirection.values()) {
                    if (!mc.world.getBlockState(check.offset(dir.toDirection(), i)).getBlock().equals(Blocks.AIR)) {
                        plant.set(false);
                        return;
                    }
                }
            });
            return plant.get();
        }
        return false;
    }

    private List<BlockPos> getBlocks(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }
        return blocks;
    }

    private List<BlockPos> findSaplings(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocc = new ArrayList<>();
        List<BlockPos> blocks = getBlocks(centerPos, radius, height);
        for (BlockPos b : blocks) if (isSapling(b)) blocc.add(b);
        return blocc;
    }

    private BlockPos findPlantedSapling() {
        List<BlockPos> saplings = findSaplings(mc.player.getBlockPos(), rRange.get(), yRange.get());
        if (saplings.isEmpty()) return null;
        saplings.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        if (sortMode.get().equals(SortMode.Farthest)) Collections.reverse(saplings);
        return saplings.get(0);
    }

    private List<BlockPos> getPlantLocations(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocc = new ArrayList<>();
        List<BlockPos> blocks = getBlocks(centerPos, radius, height);
        for (BlockPos b : blocks) if (canPlant(b)) blocc.add(b);
        return blocc;
    }

    private BlockPos findPlantLocation() {
        List<BlockPos> nearby = getPlantLocations(mc.player.getBlockPos(), rRange.get(), yRange.get());
        if (nearby.isEmpty()) return null;
        nearby.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        if (sortMode.get().equals(SortMode.Farthest)) Collections.reverse(nearby);
        return nearby.get(0);
    }

    private double distanceBetween(BlockPos pos1, BlockPos pos2) {
        double d = pos1.getX() - pos2.getX();
        double e = pos1.getY() - pos2.getY();
        double f = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public enum SortMode {Closest, Farthest}

}
