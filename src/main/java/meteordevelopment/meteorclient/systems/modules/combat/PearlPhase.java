/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.item.Items;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class PearlPhase extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");

    private final Setting<Integer> pitch = sgGeneral.add(new IntSetting.Builder()
        .name("Pitch")
        .description("How far to look downwards when throwing the pearl.")
        .defaultValue(85)
        .range(-90, 90)
        .sliderRange(0, 90)
        .build()
    );

    private final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Switches to your previous slot after throwing a pearl.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> placeBypass = sgPlace.add(new BoolSetting.Builder()
        .name("place-bypass")
        .description("Bypasses anti-cheats by placing a specific block before throwing a pearl.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Block>> bypassBlocks = sgPlace.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("What blocks to place for the bypass.")
        .defaultValue(Blocks.TRIPWIRE)
        .visible(placeBypass::get)
        .filter(this::isBypassBlock)
        .build()
    );

    public PearlPhase() {
        super(Categories.Combat, "pearl-phase", "Clip inside walls using ender pearls.");
    }

    @Override
    public void onActivate() {
        FindItemResult pearl = InvUtils.findInHotbar(Items.ENDER_PEARL);
        if (!pearl.found()) {
            error("No pearl found in hotbar.");
            toggle();
            return;
        }

        BlockPos placePos = mc.player.getBlockPos();
        FindItemResult block = InvUtils.findInHotbar(itemStack -> bypassBlocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        boolean hasBypassBlock = isBypassBlock(mc.world.getBlockState(placePos).getBlock());
        if (placeBypass.get() && !hasBypassBlock && !block.found()) {
            error("No bypass blocks found in hotbar.");
            toggle();
            return;
        }

        Rotations.rotate(getYaw(), pitch.get(), () -> throwPearl(placePos, pearl, block, hasBypassBlock));

        toggle();
    }

    private void throwPearl(BlockPos placePos, FindItemResult pearl, FindItemResult block, boolean hasBypassBlock) {
        // Place bypass block
        if (placeBypass.get() && !hasBypassBlock && block.found()) {
            InvUtils.swap(block.slot(), swapBack.get());

            BlockUtils.place(placePos, block, false, 100, false, true);
        }

        InvUtils.swap(pearl.slot(), swapBack.get());

        // Throw pearl
        mc.interactionManager.interactItem(mc.player, pearl.isOffhand() ? Hand.OFF_HAND : Hand.MAIN_HAND);

        if (swapBack.get()) InvUtils.swapBack();
    }

    private int getYaw() {
        return (int) Math.round(Rotations.getYaw(new Vec3d(Math.floor(mc.player.getX()) + 0.5, 0, Math.floor(mc.player.getZ()) + 0.5))) + 180;
    }

    private boolean isBypassBlock(Block block) {
        return block == Blocks.TRIPWIRE ||
            block == Blocks.COBWEB ||
            block == Blocks.VINE ||
            block == Blocks.SCULK_VEIN ||
            block == Blocks.GLOW_LICHEN ||
            block instanceof AbstractRailBlock ||
            block instanceof AbstractPressurePlateBlock; 
    }
}
