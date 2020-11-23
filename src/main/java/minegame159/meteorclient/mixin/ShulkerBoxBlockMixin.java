/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.ShulkerTooltip;
import minegame159.meteorclient.utils.KeyBinds;
import minegame159.meteorclient.utils.Utils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ShulkerBoxBlock.class)
public class ShulkerBoxBlockMixin {
    /**
     * @author MineGame159
     * @reason bc i want
     */
    @Overwrite
    @Environment(EnvType.CLIENT)
    public void appendTooltip(ItemStack stack, BlockView view, List<Text> tooltip, TooltipContext options) {
        CompoundTag compoundTag = stack.getSubTag("BlockEntityTag");
        if (compoundTag != null) {
            if (compoundTag.contains("LootTable", 8)) {
                tooltip.add(new LiteralText("???????"));
            }

            if (compoundTag.contains("Items", 9)) {
                DefaultedList<ItemStack> itemStacks = DefaultedList.ofSize(27, ItemStack.EMPTY);
                Inventories.fromTag(compoundTag, itemStacks);
                int totalItemStacks = 0;
                int displaysItemStacks = 0;

                if (ModuleManager.INSTANCE.get(ShulkerTooltip.class).isActive()) {
                    Map<Text, Integer> itemCounts = new HashMap<>();
                    for (ItemStack itemStack : itemStacks) {
                        if (!itemStack.isEmpty()) {
                            Text name = itemStack.getName();
                            int itemCount = itemCounts.computeIfAbsent(name, item -> 0);
                            itemCount += itemStack.getCount();
                            itemCounts.put(name, itemCount);
                        }
                    }

                    totalItemStacks = itemCounts.size();

                    List<Pair<Text, Integer>> items = new ArrayList<>(5);
                    for (int i = 0; i < ModuleManager.INSTANCE.get(ShulkerTooltip.class).lines(); i++) {
                        if (itemCounts.size() == 0) break;

                        Text bestItem = null;
                        int mostItem = 0;

                        for (Text a : itemCounts.keySet()) {
                            int b = itemCounts.get(a);
                            if (b > mostItem) {
                                mostItem = b;
                                bestItem = a;
                            }
                        }

                        items.add(new Pair<>(bestItem, mostItem));
                        itemCounts.remove(bestItem);
                    }

                    for (Pair<Text, Integer> itemCount : items) {
                        displaysItemStacks++;
                        MutableText text = itemCount.getLeft().copy();
                        text.append(" x").append(String.valueOf(itemCount.getRight()));
                        tooltip.add(text);
                    }
                } else {
                    for (ItemStack itemStack : itemStacks) {
                        if (!itemStack.isEmpty()) {
                            totalItemStacks++;

                            if (displaysItemStacks <= 4) {
                                displaysItemStacks++;
                                MutableText text = itemStack.getName().copy();
                                text.append(" x").append(String.valueOf(itemStack.getCount()));
                                tooltip.add(text);
                            }
                        }
                    }
                }

                if (totalItemStacks - displaysItemStacks > 0) {
                    tooltip.add((new TranslatableText("container.shulkerBox.more", totalItemStacks - displaysItemStacks)).formatted(Formatting.ITALIC));
                }

                tooltip.add(new LiteralText(""));
                tooltip.add(new LiteralText("Press " + Formatting.YELLOW + Utils.getKeyName(KeyBindingHelper.getBoundKeyOf(KeyBinds.SHULKER_PEEK).getCode()) + Formatting.RESET + " to peek"));
            }
        }
    }
}
