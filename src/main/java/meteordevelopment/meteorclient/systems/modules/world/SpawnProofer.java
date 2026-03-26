/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class SpawnProofer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("The tick delay between placing blocks.")
        .defaultValue(1)
        .range(0, 10)
        .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("How far away from the player you can place a block.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Double> wallsRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("How far away from the player you can place a block behind walls.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("How many blocks to place in one tick.")
        .defaultValue(1)
        .min(1)
        .build()
    );

    private final Setting<Integer> lightLevel = sgGeneral.add(new IntSetting.Builder()
        .name("light-level")
        .description("Light levels to spawn proof. Old spawning light: 7.")
        .defaultValue(0)
        .min(0)
        .sliderMax(15)
        .build()
    );

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Block to use for spawn proofing.")
        .defaultValue(Blocks.TORCH, Blocks.STONE_BUTTON, Blocks.STONE_SLAB)
        .filter(this::filterBlocks)
        .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Which spawn types should be spawn proofed.")
        .defaultValue(Mode.Both)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards the blocks being placed.")
        .defaultValue(true)
        .build()
    );

    private final Pool<BlockPos.MutableBlockPos> spawnPool = new Pool<>(BlockPos.MutableBlockPos::new);
    private final List<BlockPos.MutableBlockPos> spawns = new ArrayList<>();
    private int timer;

    public SpawnProofer() {
        super(Categories.World, "spawn-proofer", "Automatically spawnproofs unlit areas.");
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (timer < placeDelay.get()) return;

        // Find slot
        boolean foundBlock = InvUtils.testInHotbar(itemStack -> blocks.get().contains(Block.byItem(itemStack.getItem())));
        if (!foundBlock) {
            error("Found none of the chosen blocks in hotbar.");
            toggle();
            return;
        }

        // Find spawn locations
        spawnPool.freeAll(spawns);
        spawns.clear();

        BlockIterator.register((int) Math.ceil(placeRange.get()), (int) Math.ceil(placeRange.get()), (blockPos, blockState) -> {
            BlockUtils.MobSpawn spawn = BlockUtils.isValidMobSpawn(blockPos, blockState, lightLevel.get());

            if ((spawn == BlockUtils.MobSpawn.Always && (mode.get() == Mode.Always || mode.get() == Mode.Both)) ||
                    spawn == BlockUtils.MobSpawn.Potential && (mode.get() == Mode.Potential || mode.get() == Mode.Both)) {

                if (!BlockUtils.canPlace(blockPos)) return;

                // Check range and raycast
                if (isOutOfRange(blockPos)) return;

                spawns.add(spawnPool.get().set(blockPos));
            }
        });
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        // Delay
        if (timer++ < placeDelay.get()) return;

        if (spawns.isEmpty()) return;

        // Find slot
        FindItemResult block = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.byItem(itemStack.getItem())));
        if (!block.found()) {
            error("Found none of the chosen blocks in hotbar.");
            toggle();
            return;
        }

        int placedCount = 0;

        // Sort blocks to use the lowest light level spawns first
        if (isLightSource(Block.byItem(mc.player.getInventory().getItem(block.slot()).getItem()))) {
            spawns.sort(Comparator.comparingInt(blockPos -> mc.level.getMaxLocalRawBrightness(blockPos)));
            placedCount = blocksPerTick.get() - 1; // Force only one light source per tick to stop unnecessary placements
        }

        // Place blocks!
        for (BlockPos blockPos : spawns) {
            if (placedCount >= blocksPerTick.get()) continue;

            if (BlockUtils.place(blockPos, block, rotate.get(), -50, false)) {
                placedCount++;
            }
        }

        timer = 0;
    }

    private boolean isOutOfRange(BlockPos blockPos) {
        Vec3 pos = blockPos.getCenter();
        if (!PlayerUtils.isWithin(pos, placeRange.get())) return true;

        ClipContext raycastContext = new ClipContext(mc.player.getEyePosition(), pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);
        BlockHitResult result = mc.level.clip(raycastContext);
        if (result == null || !result.getBlockPos().equals(blockPos))
            return !PlayerUtils.isWithin(pos, wallsRange.get());

        return false;
    }

    private boolean filterBlocks(Block block) {
        return isNonOpaqueBlock(block) || isLightSource(block);
    }

    private boolean isNonOpaqueBlock(Block block) {
        return block instanceof ButtonBlock ||
            block instanceof SlabBlock ||
            block instanceof BasePressurePlateBlock ||
            block instanceof TransparentBlock ||
            block instanceof TripWireBlock ||
            block instanceof CarpetBlock ||
            block instanceof LeverBlock ||
            block instanceof DiodeBlock ||
            block instanceof BaseRailBlock;
    }

    private boolean isLightSource(Block block) {
        return block.defaultBlockState().getLightEmission() > 0;
    }

    public enum Mode {
        Always,
        Potential,
        Both
    }
}
