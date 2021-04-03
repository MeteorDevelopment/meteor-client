/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.player;

//Updated by squidoodly 15/06/2020

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.events.entity.player.StartBreakingBlockEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixin.AxeItemAccessor;
import minegame159.meteorclient.mixin.HoeItemAccessor;
import minegame159.meteorclient.mixin.PickaxeItemAccessor;
import minegame159.meteorclient.mixin.ShovelItemAccessor;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.*;

import java.util.HashSet;
import java.util.Set;

public class AutoTool extends Module {
    public enum EnchantPreference {
        None,
        Fortune,
        SilkTouch
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<EnchantPreference> prefer = sgGeneral.add(new EnumSetting.Builder<EnchantPreference>()
            .name("prefer")
            .description("Either to prefer Silk Touch, Fortune, or none.")
            .defaultValue(EnchantPreference.Fortune)
            .build()
    );

    private final Setting<Boolean> preferMending = sgGeneral.add(new BoolSetting.Builder()
            .name("prefer-mending")
            .description("Whether or not to prefer the Mending enchantment.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> silkTouchForEnderChest = sgGeneral.add(new BoolSetting.Builder()
            .name("silk-touch-for-ender-chest")
            .description("Mines Ender Chests only with the Silk Touch enchantment.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-break")
            .description("Stops you from breaking your tool.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> breakDurability = sgGeneral.add(new IntSetting.Builder()
            .name("anti-break-durability")
            .description("The durability to stop using a tool.")
            .defaultValue(10)
            .max(50)
            .min(2)
            .sliderMax(20)
            .build()
    );

    private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder()
            .name("switch-back")
            .description("Switches your hand to whatever was selected when releasing your attack key.")
            .defaultValue(false)
            .build()
    );

    public AutoTool() {
        super(Categories.Player, "auto-tool", "Automatically switches to the most effective tool when performing an action.");
    }

    private static final Set<Material> EMPTY_MATERIALS = new HashSet<>(0);
    private static final Set<Block> EMPTY_BLOCKS = new HashSet<>(0);

    private int prevSlot;
    private boolean wasPressed;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (switchBack.get() && !mc.options.keyAttack.isPressed() && wasPressed && prevSlot != -1) {
            mc.player.inventory.selectedSlot = prevSlot;
            prevSlot = -1;
        }

        wasPressed = mc.options.keyAttack.isPressed();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        BlockState blockState = mc.world.getBlockState(event.blockPos);
        int bestScore = -1;
        int score = 0;
        int bestSlot = -1;

        if (blockState.getHardness(mc.world, event.blockPos) < 0 || blockState.isAir()) return;

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);

            if (!isEffectiveOn(itemStack.getItem(), blockState) || shouldStopUsing(itemStack) || !(itemStack.getItem() instanceof ToolItem)) continue;

            if (silkTouchForEnderChest.get() && blockState.getBlock() == Blocks.ENDER_CHEST && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack) == 0) continue;

            score += Math.round(itemStack.getMiningSpeedMultiplier(blockState));
            score += EnchantmentHelper.getLevel(Enchantments.UNBREAKING, itemStack);
            score += EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, itemStack);

            if (preferMending.get()) score += EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack);
            if (prefer.get() == EnchantPreference.Fortune) score += EnchantmentHelper.getLevel(Enchantments.FORTUNE, itemStack);
            if (prefer.get() == EnchantPreference.SilkTouch) score += EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack);

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            if (prevSlot == -1) prevSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = bestSlot;
        }

        ItemStack currentStack = mc.player.inventory.getStack(mc.player.inventory.selectedSlot);

        if (shouldStopUsing(currentStack) && currentStack.getItem() instanceof ToolItem) {
            mc.options.keyAttack.setPressed(false);
            event.setCancelled(true);
        }
    }

    public boolean isEffectiveOn(Item item, BlockState blockState) {
        if (item.isEffectiveOn(blockState)) return true;

        Set<Material> effectiveMaterials;
        Set<Block> effectiveBlocks;

        if (item instanceof PickaxeItem) {
            effectiveMaterials = EMPTY_MATERIALS;
            effectiveBlocks = PickaxeItemAccessor.getEffectiveBlocks();
        } else if (item instanceof AxeItem) {
            effectiveMaterials = AxeItemAccessor.getEffectiveMaterials();
            effectiveBlocks = AxeItemAccessor.getEffectiveBlocks();
        } else if (item instanceof ShovelItem) {
            effectiveMaterials = EMPTY_MATERIALS;
            effectiveBlocks = ShovelItemAccessor.getEffectiveBlocks();
        } else if (item instanceof HoeItem) {
            effectiveMaterials = EMPTY_MATERIALS;
            effectiveBlocks = HoeItemAccessor.getEffectiveBlocks();
        } else if (item instanceof SwordItem) {
            effectiveMaterials = EMPTY_MATERIALS;
            effectiveBlocks = EMPTY_BLOCKS;
        } else {
            return false;
        }

        return effectiveMaterials.contains(blockState.getMaterial()) || effectiveBlocks.contains(blockState.getBlock());
    }

    private boolean shouldStopUsing(ItemStack itemStack) {
        return antiBreak.get() && itemStack.getMaxDamage() - itemStack.getDamage() < breakDurability.get();
    }
}
