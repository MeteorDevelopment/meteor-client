/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.List;

public class SpawnProofer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Range for block placement and rendering")
        .min(0)
        .sliderMax(10)
        .defaultValue(3)
        .build()
    );

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Block to use for spawn proofing")
        .defaultValue(getDefaultBlocks())
        .filter(this::filterBlocks)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks between placing blocks")
        .defaultValue(0)
        .min(0).sliderMax(10)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards the blocks being placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> alwaysSpawns = sgGeneral.add(new BoolSetting.Builder()
        .name("always-spawns")
        .description("Spawn Proofs spots that will spawn mobs")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> potentialSpawns = sgGeneral.add(new BoolSetting.Builder()
        .name("potential-spawns")
        .description("Spawn Proofs spots that will potentially spawn mobs (eg at night)")
        .defaultValue(true)
        .build()
    );


    private final Pool<Spawn> spawnPool = new Pool<Spawn>(Spawn::new);
    private final List<Spawn> spawns = new ArrayList<>();
    private int ticksWaited;

    public SpawnProofer() {
        super(Categories.World, "spawn-proofer", "Automatically spawnproofs unlit areas.");
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        // Delay
        if (delay.get() != 0 && ticksWaited < delay.get() - 1) {
            return;
        }

        // Find slot
        FindItemResult block = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        if (!block.found()) {
            error("Found none of the chosen blocks in hotbar");
            toggle();
            return;
        }


        for (Spawn spawn : spawns) spawnPool.free(spawn);
        spawns.clear();
        BlockIterator.register(range.get(), range.get(), (blockPos, blockState) -> {
            if (validSpawn(blockPos)) spawns.add(spawnPool.get().set(blockPos));
        });
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        // Delay
        if (delay.get() != 0 && ticksWaited < delay.get() - 1) {
            ticksWaited++;
            return;
        }
        if (spawns.isEmpty()) return;


        // Find slot
        FindItemResult block = InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));

        // Place blocks
        if (delay.get() == 0) {
            for (Spawn spawn : spawns) BlockUtils.place(spawn.blockPos, block, rotate.get(), -50, false);
        } else {

            // Check if light source
            if (isLightSource(Block.getBlockFromItem(mc.player.getInventory().getStack(block.getSlot()).getItem()))) {

                // Find lowest light level
                int lowestLightLevel = 16;
                Spawn selectedSpawn = spawns.get(0);
                for (Spawn spawn : spawns) {
                    int lightLevel = mc.world.getLightLevel(spawn.blockPos);
                    if (lightLevel < lowestLightLevel) {
                        lowestLightLevel = lightLevel;
                        selectedSpawn = spawn;
                    }
                }

                BlockUtils.place(selectedSpawn.blockPos, block, rotate.get(), -50, false);

            } else {

                BlockUtils.place(spawns.get(0).blockPos, block, rotate.get(), -50, false);

            }

        }

        ticksWaited = 0;
    }

    private boolean validSpawn(BlockPos blockPos) { // Copied from Light Overlay and modified slightly
        BlockState blockState = mc.world.getBlockState(blockPos);

        if (blockPos.getY() == 0) return false;
        if (!(blockState.getBlock() instanceof AirBlock)) return false;

        if (!topSurface(mc.world.getBlockState(blockPos.down()))) {
            if (mc.world.getBlockState(blockPos.down()).getCollisionShape(mc.world, blockPos.down()) != VoxelShapes.fullCube()) return false;
            if (mc.world.getBlockState(blockPos.down()).isTranslucent(mc.world, blockPos.down())) return false;
        }

        if (mc.world.getLightLevel(blockPos, 0) <= 7) return potentialSpawns.get();
        else if (mc.world.getLightLevel(LightType.BLOCK, blockPos) <= 7) return alwaysSpawns.get();

        return false;
    }

    private boolean topSurface(BlockState blockState) { // Copied from Light Overlay
        if (blockState.getBlock() instanceof SlabBlock && blockState.get(SlabBlock.TYPE) == SlabType.TOP) return true;
        else return blockState.getBlock() instanceof StairsBlock && blockState.get(StairsBlock.HALF) == BlockHalf.TOP;
    }

    private List<Block> getDefaultBlocks() {

        ArrayList<Block> defaultBlocks = new ArrayList<>();
        for (Block block : Registry.BLOCK) {
            if (filterBlocks(block)) defaultBlocks.add(block);
        }
        return defaultBlocks;
    }

    private boolean filterBlocks(Block block) {
        return isNonOpaqueBlock(block) || isLightSource(block);
    }

    private boolean isNonOpaqueBlock(Block block) {
        return block instanceof AbstractButtonBlock ||
            block instanceof SlabBlock ||
            block instanceof AbstractPressurePlateBlock ||
            block instanceof TransparentBlock ||
            block instanceof TripwireBlock;
    }

    private boolean isLightSource(Block block) {
        return block.getDefaultState().getLuminance() > 0;
    }

    private static class Spawn {
        public BlockPos.Mutable blockPos = new BlockPos.Mutable();

        public Spawn set(BlockPos blockPos) {
            this.blockPos.set(blockPos);

            return this;
        }
    }
}
