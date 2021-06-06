/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.world;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.player.AutoTool;
import minegame159.meteorclient.utils.player.FindItemResult;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class EChestFarmer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("self-toggle")
        .description("Disables when you reach the desired amount of obsidian.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreExisting = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-existing")
        .description("Ignores existing obsidian in your inventory and mines the total target amount.")
        .defaultValue(true)
        .visible(selfToggle::get)
        .build()
    );

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
        .name("amount")
        .description("The amount of obsidian to farm.")
        .defaultValue(64)
        .sliderMax(128)
        .min(8).max(512)
        .visible(selfToggle::get)
        .build()
    );

    private int startCount;

    public EChestFarmer() {
        super(Categories.World, "echest-farmer", "Places and mines Ender Chests where you're looking.");
    }

    @Override
    public void onActivate() {
        startCount = InvUtils.find(Items.OBSIDIAN).getCount();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (selfToggle.get() && InvUtils.find(Items.OBSIDIAN).getCount() - (ignoreExisting.get() ? startCount : 0) >= amount.get()) {
            toggle();
            return;
        }

        if (mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();

        if (mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST) {
            FindItemResult pick = InvUtils.findInHotbar(itemStack -> AutoTool.isEffectiveOn(itemStack.getItem(), Blocks.ENDER_CHEST.getDefaultState())
                && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack) == 0);
            if (!pick.found()) return;

            InvUtils.swap(pick.getSlot());
            mc.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
        } else if (mc.world.getBlockState(pos.up()).getMaterial().isReplaceable()) {
            BlockUtils.place(pos.up(), InvUtils.findInHotbar(Items.ENDER_CHEST), false, 0, true);
        }
    }
}
