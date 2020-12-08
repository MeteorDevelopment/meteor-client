/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PreTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.BlockIterator;
import minegame159.meteorclient.utils.PlayerUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public class LiquidFiller extends ToggleModule {
    public enum PlaceIn {
        Lava,
        Water,
        Both
    }

    private final SettingGroup sgGeneral  = settings.getDefaultGroup();

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
            .description("Select which blocks it will use to place.")
            .defaultValue(new ArrayList<>())
            .build()
    );

    private final Setting<PlaceIn> placeInLiquids = sgGeneral.add(new EnumSetting.Builder<PlaceIn>()
            .name("place-in")
            .description("Which liquids to place in.")
            .defaultValue(PlaceIn.Lava)
            .build()
    );

    public LiquidFiller(){
        super(Category.Misc, "Liquid-Filler", "Places blocks inside of liquid source blocks within range of you.");
    }

    @EventHandler
    private final Listener<PreTickEvent> onTick = new Listener<>(event -> BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos, blockState) -> {
        if (blockState.getFluidState().getLevel() == 8 && blockState.getFluidState().isStill()) {
            Block liquid = blockState.getBlock();

            PlaceIn placeIn = placeInLiquids.get();
            if (placeIn == PlaceIn.Both || (placeIn == PlaceIn.Lava && liquid == Blocks.LAVA) || (placeIn == PlaceIn.Water && liquid == Blocks.WATER)) {
                if (PlayerUtils.placeBlock(blockPos, findSlot(), Hand.MAIN_HAND)) BlockIterator.disableCurrent();
            }
        }
    }));

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