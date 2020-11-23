/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

//Updated by squidoodly 15/06/2020

import me.zero.alpine.event.EventPriority;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.events.StartBreakingBlockEvent;
import minegame159.meteorclient.mixin.AxeItemAccessor;
import minegame159.meteorclient.mixin.HoeItemAccessor;
import minegame159.meteorclient.mixin.PickaxeItemAccessor;
import minegame159.meteorclient.mixin.ShovelItemAccessor;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashSet;
import java.util.Set;

public class AutoTool extends ToggleModule {
    private static final Set<Material> EMPTY_MATERIALS = new HashSet<>(0);
    private static final Set<Block> EMPTY_BLOCKS = new HashSet<>(0);

    public enum Prefer {
        None,
        Fortune,
        SilkTouch
    }
    public enum materialPreference{
        None,
        Same,
        Best
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Prefer> prefer = sgGeneral.add(new EnumSetting.Builder<Prefer>()
            .name("prefer")
            .description("Prefer silk touch, fortune or none.")
            .defaultValue(Prefer.Fortune)
            .build()
    );

    private final Setting<Boolean> preferMending = sgGeneral.add(new BoolSetting.Builder()
            .name("prefer-mending")
            .description("Prefers mending.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> enderChestOnlyWithSilkTouch = sgGeneral.add(new BoolSetting.Builder()
            .name("ender-chest-only-with-silk-touch")
            .description("Mine ender chest only with silk touch.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> antiBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-break")
            .description("Stops you from breaking your weapon.")
            .defaultValue(false)
            .build()
    );

    private final Setting<materialPreference> material = sgGeneral.add(new EnumSetting.Builder<materialPreference>().name("material-preference")
            .description("How the AntiBreak decides what to replace your tool with")
            .defaultValue(materialPreference.Best)
            .build()
    );

    public AutoTool() {
        super(Category.Player, "auto-tool", "Automatically switches to the most effective tool when breaking blocks.");
    }

    private BlockState blockState = null;

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if(mc.player.getMainHandStack().getItem() instanceof ToolItem && antiBreak.get()
                && (mc.player.getMainHandStack().getItem().getMaxDamage() - mc.player.getMainHandStack().getDamage()) <= 11){
            int slot = -1;
            int score = 0;
            for(int i = 0; i < 36; i++){
                if ((mc.player.inventory.getStack(i).getMaxDamage() - mc.player.inventory.getStack(i).getDamage()) <= 11) continue;
                if(material.get() == materialPreference.None && mc.player.inventory.getStack(i).getItem().getClass() == mc.player.getMainHandStack().getItem().getClass()){
                    slot = i;
                    break;
                }else if(material.get() == materialPreference.Same && mc.player.inventory.getStack(i).getItem() == mc.player.getMainHandStack().getItem()){
                    slot = i;
                    break;
                }else if(material.get() == materialPreference.Best && blockState != null){
                    if(mc.player.inventory.getStack(i).getItem().getClass() == mc.player.getMainHandStack().getItem().getClass()){
                        if(score < Math.round(mc.player.inventory.getStack(i).getMiningSpeedMultiplier(blockState))){
                            score = Math.round(mc.player.inventory.getStack(i).getMiningSpeedMultiplier(blockState));
                            slot = i;
                        }
                    }
                }else if (material.get() == materialPreference.Best && blockState == null) break;
            }
            if(slot == -1 && material.get() != materialPreference.None){
                for(int i = 0; i < 36; i++){
                    if(mc.player.inventory.getStack(i).getItem().getClass() == mc.player.getMainHandStack().getItem().getClass()
                            && (mc.player.inventory.getStack(i).getMaxDamage() - mc.player.inventory.getStack(i).getDamage()) > 11){
                        slot = i;
                        break;
                    }
                }
            }
            if(slot != -1){
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(mc.player.inventory.selectedSlot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(slot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(mc.player.inventory.selectedSlot), 0, SlotActionType.PICKUP);
            }else if(mc.player.inventory.getEmptySlot() != -1){
                int emptySlot = mc.player.inventory.getEmptySlot();
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(mc.player.inventory.selectedSlot), 0, SlotActionType.PICKUP);
                InvUtils.clickSlot(InvUtils.invIndexToSlotId(emptySlot), 0, SlotActionType.PICKUP);
            }else {
                if (mc.player.inventory.selectedSlot < 8) mc.player.inventory.selectedSlot = mc.player.inventory.selectedSlot + 1;
                else mc.player.inventory.selectedSlot = mc.player.inventory.selectedSlot - 1;
            }
        }
    });

    @EventHandler
    private final Listener<StartBreakingBlockEvent> onStartBreakingBlock = new Listener<>(event -> {
        blockState = mc.world.getBlockState(event.blockPos);
        int bestScore = -1;
        int bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);
            if (!isEffectiveOn(itemStack.getItem(), blockState) || (itemStack.getMaxDamage() - itemStack.getDamage() <= 11)) continue;
            int score = 0;

            if (enderChestOnlyWithSilkTouch.get() && blockState.getBlock() == Blocks.ENDER_CHEST && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack) == 0) continue;

            score += Math.round(itemStack.getMiningSpeedMultiplier(blockState));
            score += EnchantmentHelper.getLevel(Enchantments.UNBREAKING, itemStack);
            score += EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, itemStack);
            if (preferMending.get()) score += EnchantmentHelper.getLevel(Enchantments.MENDING, itemStack);
            if (prefer.get() == Prefer.Fortune) score += EnchantmentHelper.getLevel(Enchantments.FORTUNE, itemStack);
            if (prefer.get() == Prefer.SilkTouch) score += EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, itemStack);

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            mc.player.inventory.selectedSlot = bestSlot;
        }
    }, EventPriority.HIGH);

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
}
