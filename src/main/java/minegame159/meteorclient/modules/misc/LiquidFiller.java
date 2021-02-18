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
import minegame159.meteorclient.utils.world.BlockIterator;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import java.util.ArrayList;
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

    private final Setting<List<Block>> whitelist = sgGeneral.add(new BlockListSetting.Builder()
            .name("block-whitelist")
            .description("The allowed blocks that it will use to fill up the liquid.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the space targeted for filling.")
            .defaultValue(true)
            .build()
    );

    public LiquidFiller(){
        super(Categories.Misc, "liquid-filler", "Places blocks inside of liquid source blocks within range of you.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        int slot = findSlot();

        if (slot != -1) {
            BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos, blockState) -> {
                if (blockState.getFluidState().getLevel() == 8 && blockState.getFluidState().isStill()) {
                    Block liquid = blockState.getBlock();

                    PlaceIn placeIn = placeInLiquids.get();
                    if (placeIn == PlaceIn.Both || (placeIn == PlaceIn.Lava && liquid == Blocks.LAVA) || (placeIn == PlaceIn.Water && liquid == Blocks.WATER)) {
                        if (BlockUtils.place(blockPos, Hand.MAIN_HAND, slot, rotate.get(), 0, true)) {
                            BlockIterator.disableCurrent();
                        }
                    }
                }
            });
        }
    }

    private int findSlot() {
        int slot = -1;

        for (int i = 0; i < 9; i++){
            ItemStack block = mc.player.inventory.getStack(i);
            if ((block.getItem() instanceof BlockItem) && whitelist.get().contains(Block.getBlockFromItem(block.getItem()))) {
                slot = i;
                break;
            }
        }

        return slot;
    }
}