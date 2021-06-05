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
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.List;

public class SpawnProofer extends Module{
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("Range for block placement and rendering")
            .min(1)
            .max(4)
            .sliderMin(1)
            .sliderMax(1)
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
    
    private final Setting<Boolean> multiPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("multi-place")
            .description("Places multiple blocks per tick")
            .defaultValue(false)
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
            .min(0).max(10)
            .build()
    );
    
    private final Setting<Boolean> spawnProofAlwaysSpawns = sgGeneral.add(new BoolSetting.Builder()
            .name("always-spawns")
            .description("Spawn proofs places where mobs can spawn")
            .defaultValue(true)
            .build()
    );
    
    private final Setting<Boolean> spawnProofPotentialSpawns = sgGeneral.add(new BoolSetting.Builder()
            .name("potential-spawns")
            .description("Spawn proofs places where mobs can potentially spawn (eg at night)")
            .defaultValue(true)
            .build()
    );
    
    
    private final ArrayList<BlockPos> positions = new ArrayList<>();
    private int ticksWaited;
    
    public SpawnProofer() {
        super(Categories.World, "spawning-spots", "Shows and spawn proofs spots where mobs can spawn (better with LightOverlay active)");
    }
    
    @Override
    public void onActivate() {
        ticksWaited = 0;
    }
    
    @Override
    public void onDeactivate() {
        ticksWaited = 0;
    }
    
    @EventHandler
    private void onTick(TickEvent.Post event) {
        // Clear and set positions
        positions.clear();
        BlockIterator.register(range.get(), range.get(), (blockPos, blockState) -> {
            if (validSpawn(blockPos)) positions.add(blockPos);
        });
        
        if (positions.size() == 0) return;
        
        // Tick delay
        if (ticksWaited < delay.get()) {
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
        
        // If is light source
        if (isLightSource(Block.getBlockFromItem(
                mc.player.inventory.getStack(slot).getItem()
        ))) {
            
            // Find lowest light level block
            int lowestLightLevel = 16;
            BlockPos selectedBlockPos = positions.get(0); // Just for initialization
            for (BlockPos blockPos : positions) {
                
                int lightLevel = mc.world.getLightLevel(blockPos);
                if (lightLevel < lowestLightLevel) {
                    lowestLightLevel = lightLevel;
                    selectedBlockPos = blockPos;
                }
            }
            
            BlockUtils.place(selectedBlockPos, Hand.MAIN_HAND, slot, rotate.get(), -50, false);
            
        } else {
            
            // Place first in positions
            BlockUtils.place(positions.get(0), Hand.MAIN_HAND, slot, rotate.get(), -50, false);
            
            // Multiplace
            if (multiPlace.get()) {
                slot = findSlot();
                
                positions.remove(0);
                for (BlockPos blockPos : positions) BlockUtils.place(blockPos, Hand.MAIN_HAND, slot, rotate.get(), -50, false);
            }
        }
        
        // Reset tick delay
        ticksWaited = 0;
    }
    
    private int findSlot() {
        return InvUtils.findItemInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
    }
    
    private boolean validSpawn(BlockPos blockPos) { // Copied from Light Overlay and modified slightly
        BlockState blockState = mc.world.getBlockState(blockPos);
        
        if (blockPos.getY() == 0) return false;
        if (!(blockState.getBlock() instanceof AirBlock)) return false;
        
        if (!topSurface(mc.world.getBlockState(blockPos.down()))) {
            if (mc.world.getBlockState(blockPos.down()).getCollisionShape(mc.world, blockPos.down()) != VoxelShapes.fullCube()) return false;
            if (mc.world.getBlockState(blockPos.down()).isTranslucent(mc.world, blockPos.down())) return false;
        }
        
        if (mc.world.getLightLevel(blockPos, 0) <= 7) return spawnProofPotentialSpawns.get();
        else if (mc.world.getLightLevel(LightType.BLOCK, blockPos) <= 7) return spawnProofAlwaysSpawns.get();
        
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
                block instanceof GlassBlock ||
                block instanceof TripwireBlock;
    }
    
    private boolean isLightSource(Block block) {
            return block instanceof TorchBlock;
    }
}
