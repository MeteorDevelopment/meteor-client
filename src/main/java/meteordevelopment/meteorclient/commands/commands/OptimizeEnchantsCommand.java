/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.EnchantmentLevelArgumentType;
import meteordevelopment.meteorclient.commands.arguments.RegistryEntryReferenceArgumentType;
import meteordevelopment.meteorclient.utils.misc.EnchantmentOptimizer;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;


import java.util.ArrayList;
import java.util.List;

public class OptimizeEnchantsCommand extends Command {
    public OptimizeEnchantsCommand() {
        super("optimize-enchant", "Calculates the optimal order to apply enchantments for minimum XP cost.", "eopt");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        // TODO: The optimizer supports book-only mode (item=null) for combining enchanted books,
        // but this command currently requires an item argument, so item will never be null.
        // Should we add an item-less version of the command for book-only optimization?

        // TODO: should we restrict the available items to only those that can be enchanted?
        // e.g. armors, weapons, tools, books, etc.
        builder.then(argument("item", ItemArgument.item(REGISTRY_ACCESS))
                .then(buildEnchantmentChain(1, 20))
            // TODO: what should the max depth be? Idk how many enchantments on a single item MC supports.
        );
    }

    /**
     * Recursively builds a chain of enchantment arguments.
     * Each enchantment requires a name and level, and can optionally chain to the next.
     */
    private RequiredArgumentBuilder<ClientSuggestionProvider, ?> buildEnchantmentChain(int index, int maxDepth) {
        String enchantArg = "enchantment" + index;
        String levelArg = "level" + index;

        var enchantmentArg = argument(enchantArg, RegistryEntryReferenceArgumentType.enchantment());
        var levelArgBuilder = argument(levelArg, EnchantmentLevelArgumentType.enchantmentLevel(enchantArg))
            .executes(context -> {
                executeOptimization(context, index);
                return SINGLE_SUCCESS;
            });

        if (index < maxDepth) {
            levelArgBuilder.then(buildEnchantmentChain(index + 1, maxDepth));
        }

        return enchantmentArg.then(levelArgBuilder);
    }

    /**
     * Extracts all enchantments from context and runs optimization.
     */
    private void executeOptimization(CommandContext<ClientSuggestionProvider> context, int enchantmentCount) {
        try {
            Item item = getItem(context);
            List<EnchantmentOptimizer.EnchantmentEntry> enchants = new ArrayList<>();

            for (int i = 1; i <= enchantmentCount; i++) {
                String enchantArg = "enchantment" + i;
                String levelArg = "level" + i;

                try {
                    Holder.Reference<Enchantment> enchantment =
                        RegistryEntryReferenceArgumentType.getEnchantment(context, enchantArg);
                    int level = IntegerArgumentType.getInteger(context, levelArg);

                    // TODO: keep validation or allow arbitary levels? MC supports up to 255.
                    // Validate level against enchantment's max level
                    int maxLevel = enchantment.value().getMaxLevel();
                    if (level > maxLevel) {
                        String enchantName = enchantment.value().description().getString();
                        error("Enchantment (highlight)%s(default) has max level (highlight)%d(default), but you specified (highlight)%d(default).",
                            enchantName, maxLevel, level);
                        return;
                    }

                    enchants.add(new EnchantmentOptimizer.EnchantmentEntry(enchantment, level));
                } catch (IllegalArgumentException _) {
                    // Argument doesn't exist, we've reached the end
                    break;
                }
            }

            if (enchants.isEmpty()) {
                error("No enchantments specified.");
                return;
            }

            optimize(item, enchants);
        } catch (Exception e) {
            error("Failed to parse enchantments: %s", e.getMessage());
        }
    }

    private Item getItem(CommandContext<ClientSuggestionProvider> context) {
        try {
            var itemArg = ItemArgument.getItem(context, "item");
            return itemArg.item().value();
        } catch (Exception _) {
            return null;
        }
    }

    private void optimize(Item item, List<EnchantmentOptimizer.EnchantmentEntry> enchants) {
        try {
            var registry = mc.getConnection().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            EnchantmentOptimizer.OptimizationResult result = EnchantmentOptimizer.create(registry).optimize(item, enchants);

            String itemName = item != null ? item.components().get(DataComponents.ITEM_NAME).getString() : "Book";

            MutableComponent msg = Component.empty();

            // Header
            msg.append(Component.literal("=== Enchantment Optimization for " + itemName + " ===\n").withStyle(ChatFormatting.GOLD));

            msg.append(Component.literal("Total Cost: %d levels (%d XP)\n".formatted(result.totalLevels(), result.totalXp())).withStyle(ChatFormatting.YELLOW));

            if (result.instructions().isEmpty()) {
                msg.append(Component.literal("No combinations needed - single enchantment only.").withStyle(ChatFormatting.GRAY));

                ChatUtils.sendMsg(msg);
                return;
            }

            msg.append(Component.literal("Steps:\n").withStyle(ChatFormatting.AQUA));

            for (int i = 0; i < result.instructions().size(); i++) {
                EnchantmentOptimizer.Instruction instr = result.instructions().get(i);

                msg.append(Component.literal("  " + (i + 1) + ". Combine ").withStyle(ChatFormatting.GRAY));
                msg.append(formatItem(instr.left()).copy().withStyle(ChatFormatting.YELLOW));
                msg.append(Component.literal(" with ").withStyle(ChatFormatting.GRAY));
                msg.append(formatItem(instr.right()).copy().withStyle(ChatFormatting.AQUA));
                msg.append(Component.literal(
                    "\n     Cost: %d levels (%d XP), Prior Work Penalty: %d\n".formatted(
                        instr.levels(),
                        instr.xp(),
                        instr.priorWorkPenalty()
                    )
                ).withStyle(ChatFormatting.GRAY));
            }

            ChatUtils.sendMsg(msg);
        } catch (Exception e) {
            error("Failed to optimize enchantments: %s", e.getMessage());
        }
    }

    private Component formatItem(EnchantmentOptimizer.Combination comb) {
        String baseName = comb.baseItem != null
            ? comb.baseItem.components().get(DataComponents.ITEM_NAME).getString()
            : "Book";

        List<String> enchantments = new ArrayList<>();
        collectEnchantments(comb, enchantments);

        if (enchantments.isEmpty()) return Component.literal(baseName);

        // Format: "ItemName (ench1, ench2, ench3)"
        return Component.literal(baseName + " (" + String.join(", ", enchantments) + ")");
    }

    private void collectEnchantments(EnchantmentOptimizer.Combination comb, List<String> out) {
        // Leaf node - single enchantment
        if (comb.enchantment != null) {
            out.add(comb.enchantment.value().description().getString() + " " + romanNumeral(comb.level));
            return;
        }

        // Merged node - collect from both children
        if (comb.left != null && comb.right != null) {
            collectEnchantments(comb.left, out);
            collectEnchantments(comb.right, out);
        }
    }

    private String romanNumeral(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(num);
        };
    }
}
