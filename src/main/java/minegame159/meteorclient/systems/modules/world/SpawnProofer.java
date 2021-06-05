/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.world;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.world.BlockIterator;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public class SpawnProofer extends Module{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("Range for block placement and rendering")
            .min(0).max(10)
            .defaultValue(4)
            .build()
    );
    
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Block to use for spawn proofing")
            .defaultValue(getDefaultBlocks())
            .filter(this::filterBlocks)
            .build()
    );
    
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates towards the blocks being placed.")
            .defaultValue(true)
            .build()
    );
    
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay in ticks between placing blocks")
            .defaultValue(0)
            .min(0)
            .build()
    );
    
    private final Setting<Boolean> alwaysSpawns = sgGeneral.add(new BoolSetting.Builder()
            .name("always-spawns")
            .description("Spawn proofs places where mobs can spawn")
            .defaultValue(true)
            .build()
    );
    
    private final Setting<Boolean> potentialSpawns = sgGeneral.add(new BoolSetting.Builder()
            .name("potential-spawns")
            .description("Spawn proofs places where mobs can potentially spawn (eg at night)")
            .defaultValue(true)
            .build()
    );
    
    
    private final ArrayList<BlockPos> positions = new ArrayList<>();
    private int ticksWaited;
    
    public SpawnProofer() {
        super(Categories.World, "spawning-spots", "Spawn proofs spots where mobs can spawn (Better with LightOverlay active)");
    }
    
    @EventHandler
    private void onTick(TickEvent.Pre event) {
    
        // Tick delay
        if (delay.get() != 0 || ticksWaited < delay.get() - 1) {
            ticksWaited++;
            return;
        }
    
        // Find slot
        int slot = findSlot();
        if (slot == -1) {
            error("Found none of the chosen blocks in hotbar");
            toggle();
            return;
        }
        
        // Clear and set positions
        positions.clear();
        BlockIterator.register(range.get(), range.get(), (blockPos, blockState) -> {
            BlockUtils.MobSpawning spawn = BlockUtils.validSpawn(blockPos);
            if ((alwaysSpawns.get() && spawn == BlockUtils.MobSpawning.Always) ||
                    (potentialSpawns.get() && spawn == BlockUtils.MobSpawning.Potential)) positions.add(blockPos);
        });
        
        // Place blocks
        BlockIterator.after(() -> {
            if (delay.get() == 0) {
                
                for (BlockPos blockPos : positions) BlockUtils.place(blockPos, Hand.MAIN_HAND, slot, rotate.get(), -50, false);
                
            } else {
    
                if (isLightSource(Block.getBlockFromItem(mc.player.inventory.getStack(slot).getItem()))) {
    
                    // Find lowest light level block
                    int lowestLightLevel = 16;
                    BlockPos selectedBlockPos = positions.get(0);
                    for (BlockPos blockPos : positions) {
        
                        int lightLevel = mc.world.getLightLevel(blockPos);
                        if (lightLevel < lowestLightLevel) {
                            lowestLightLevel = lightLevel;
                            selectedBlockPos = blockPos;
                        }
                    }
    
                    BlockUtils.place(selectedBlockPos, Hand.MAIN_HAND, slot, rotate.get(), -50, false);
    
                } else {
                    
                    BlockUtils.place(positions.get(0), Hand.MAIN_HAND, slot, rotate.get(), -50, false);
                    
                }
            }
        });
        
        
        // Reset tick delay
        ticksWaited = 0;
    }
    
    private int findSlot() {
        return InvUtils.findItemInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
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
}
