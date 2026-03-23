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
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.RaycastContext;

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

    private final Pool<BlockPos.Mutable> spawnPool = new Pool<>(BlockPos.Mutable::new);
    private final List<BlockPos.Mutable> spawns = new ArrayList<>();
    private int timer;

    public SpawnProofer() {
        super(Categories.World, "spawn-proofer", "Automatically spawnproofs unlit areas.");
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (timer < placeDelay.get()) return;

        // Find slot
        boolean foundBlock = InvUtils.testInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
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
        FindItemResult block = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        if (!block.found()) {
            error("Found none of the chosen blocks in hotbar.");
            toggle();
            return;
        }

        int placedCount = 0;

        // Sort blocks to use the lowest light level spawns first
        if (isLightSource(Block.getBlockFromItem(mc.player.getInventory().getStack(block.slot()).getItem()))) {
            spawns.sort(Comparator.comparingInt(blockPos -> mc.world.getLightLevel(blockPos)));
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
        Vec3d pos = blockPos.toCenterPos();
        if (!PlayerUtils.isWithin(pos, placeRange.get())) return true;

        RaycastContext raycastContext = new RaycastContext(mc.player.getEyePos(), pos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
        BlockHitResult result = mc.world.raycast(raycastContext);
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
            block instanceof AbstractPressurePlateBlock ||
            block instanceof TransparentBlock ||
            block instanceof TripwireBlock ||
            block instanceof CarpetBlock ||
            block instanceof LeverBlock ||
            block instanceof AbstractRedstoneGateBlock ||
            block instanceof AbstractRailBlock;
    }

    private boolean isLightSource(Block block) {
        return block.getDefaultState().getLuminance() > 0;
    }

    public enum Mode {
        Always,
        Potential,
        Both
    }
}
