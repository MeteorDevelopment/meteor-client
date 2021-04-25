/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.world;

//Created by squidoodly 20/06/2020

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.player.AutoTool;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class EChestFarmer extends Module {
    private static final BlockState ENDER_CHEST = Blocks.ENDER_CHEST.getDefaultState();

    private final SettingGroup sgGeneral  = settings.getDefaultGroup();

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
            .name("target-amount")
            .description("The amount of obsidian to farm.")
            .defaultValue(64)
            .min(8)
            .sliderMax(64)
            .max(512)
            .build()
    );

    private final Setting<Integer> lowerAmount = sgGeneral.add(new IntSetting.Builder()
            .name("lower-amount")
            .description("The specified amount before this module toggles on again.")
            .defaultValue(8)
            .min(0)
            .max(64)
            .sliderMax(32)
            .build()
    );

    private final Setting<Boolean> disableOnAmount = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-completion")
            .description("Whether or not to disable when you reach your desired amount of stacks of obsidian.")
            .defaultValue(true)
            .build()
    );

    private boolean stop = false;
    private int numLeft = Math.floorDiv(amount.get() , 8);

    public EChestFarmer(){
        super(Categories.World, "EChest-farmer", "Places and mines Ender Chests where you're looking.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (lowerAmount.get() < InvUtils.findItemWithCount(Items.OBSIDIAN).count) stop = false;
        if (stop && !disableOnAmount.get()) {
            stop = false;
            numLeft = Math.floorDiv(amount.get(), 8);
            return;
        } else if (stop && disableOnAmount.get()) {
            this.toggle();
            return;
        }
        InvUtils.FindItemResult itemResult = InvUtils.findItemWithCount(Items.ENDER_CHEST);
        int slot = -1;
        if(itemResult.count != 0 && itemResult.slot < 9 && itemResult.slot != -1) {
            for (int i = 0; i < 9; i++) {
                if (Modules.get().get(AutoTool.class).isEffectiveOn(mc.player.inventory.getStack(i).getItem(), ENDER_CHEST)
                        && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, mc.player.inventory.getStack(i)) == 0) {
                    slot = i;
                }
            }
            if (slot != -1 && itemResult.slot != -1 && itemResult.slot < 9) {
                if (mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
                BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
                if(mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST){
                    if (mc.player.inventory.selectedSlot != slot) {
                        mc.player.inventory.selectedSlot = slot;
                    }
                    mc.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
                    numLeft -= 1;
                    if(numLeft == 0){
                        stop = true;
                    }
                } else if (mc.world.getBlockState(pos.up()).getBlock() == Blocks.AIR) {
                    BlockUtils.place(pos.up(), Hand.MAIN_HAND, itemResult.slot, false, 0, true);
                }

            }
        }
    }
}
