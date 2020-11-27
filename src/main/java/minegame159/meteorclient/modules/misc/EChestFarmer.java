/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

//Created by squidoodly 20/06/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.player.AutoTool;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.PlayerUtils;
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

public class EChestFarmer extends ToggleModule {
    private static final BlockState ENDER_CHEST = Blocks.ENDER_CHEST.getDefaultState();

    public EChestFarmer(){
        super(Category.Misc, "EChest-farmer", "Places and mines EChests where you are looking.");
    }

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
            .description("The amount before this turns on again.")
            .defaultValue(8)
            .min(0)
            .max(64)
            .sliderMax(32)
            .build()
    );

    private final Setting<Boolean> disableOnAmount = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-completion")
            .description("Whether to disable once you reach target stacks")
            .defaultValue(true)
            .build()
    );

    private boolean stop = false;
    private int numLeft = Math.floorDiv(amount.get() , 8);

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
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
                if (ModuleManager.INSTANCE.get(AutoTool.class).isEffectiveOn(mc.player.inventory.getStack(i).getItem(), ENDER_CHEST)
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
                    if (mc.player.inventory.selectedSlot != itemResult.slot)
                    mc.player.inventory.selectedSlot = itemResult.slot;
                    PlayerUtils.placeBlock(pos.up(), Hand.MAIN_HAND);
                }

            }
        }
    });
}
