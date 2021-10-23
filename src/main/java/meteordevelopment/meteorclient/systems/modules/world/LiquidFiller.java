/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;

import java.util.List;

public class LiquidFiller extends Module {
    public enum PlaceIn {
        Lava,
        Water,
        Both
    }

    private final SettingGroup sgGeneral  = settings.getDefaultGroup();

    private final Setting<PlaceIn> placeInLiquids = sgGeneral.add(new EnumSetting.Builder<PlaceIn>()
            .name("place-in")
            .description("What type of liquids to place in.")
            .defaultValue(PlaceIn.Lava)
            .build()
    );

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-radius")
            .description("Horizontal radius in which to search for liquids.")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("vertical-radius")
            .description("Vertical radius in which to search for liquids.")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay between actions in ticks.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private final Setting<List<Block>> whitelist = sgGeneral.add(new BlockListSetting.Builder()
            .name("block-whitelist")
            .description("The allowed blocks that it will use to fill up the liquid.")
            .defaultValue(
                Blocks.DIRT,
                Blocks.COBBLESTONE,
                Blocks.STONE,
                Blocks.NETHERRACK,
                Blocks.DIORITE,
                Blocks.GRANITE,
                Blocks.ANDESITE
            )
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the space targeted for filling.")
            .defaultValue(true)
            .build()
    );

    private int timer;

    public LiquidFiller(){
        super(Categories.World, "liquid-filler", "Places blocks inside of liquid source blocks within range of you.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Update timer according to delay
        if (timer < delay.get()) {
            timer++;
            return;
        } else {
            timer = 0;
        }

        // Find slot with a block
        FindItemResult item = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && whitelist.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        if (!item.found()) return;

        // Loop blocks around the player
        BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos, blockState) -> {
            // Check if the block a source liquid block
            if (isSource(blockState)) {
                Block liquid = blockState.getBlock();

                PlaceIn placeIn = placeInLiquids.get();
                if (placeIn == PlaceIn.Both || (placeIn == PlaceIn.Lava && liquid == Blocks.LAVA) || (placeIn == PlaceIn.Water && liquid == Blocks.WATER)) {
                    if (BlockUtils.place(blockPos, item, rotate.get(), 0, true)) {
                        BlockIterator.disableCurrent();
                    }
                }
            }
        });
    }

    private boolean isSource(BlockState blockState) {
        return blockState.getFluidState().getLevel() == 8 && blockState.getFluidState().isStill();
    }
}
