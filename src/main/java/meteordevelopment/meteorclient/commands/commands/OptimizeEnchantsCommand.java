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
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class OptimizeEnchantsCommand extends Command {
    public OptimizeEnchantsCommand() {
        super("optimize-enchant", "Calculates the optimal order to apply enchantments for minimum XP cost.", "eopt");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // TODO: The optimizer supports book-only mode (item=null) for combining enchanted books,
        // but this command currently requires an item argument, so item will never be null.
        // Should we add an item-less version of the command for book-only optimization?

        // TODO: should we restrict the available items to only those that can be enchanted?
        // e.g. armors, weapons, tools, books, etc.
        builder.then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS))
                .then(buildEnchantmentChain(1, 20))
            // TODO: what should the max depth be? Idk how many enchantments on a single item MC supports.
        );
    }

    /**
     * Recursively builds a chain of enchantment arguments.
     * Each enchantment requires a name and level, and can optionally chain to the next.
     */
    private RequiredArgumentBuilder<CommandSource, ?> buildEnchantmentChain(int index, int maxDepth) {
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
    private void executeOptimization(CommandContext<CommandSource> context, int enchantmentCount) {
        try {
            Item item = getItem(context);
            List<EnchantmentOptimizer.EnchantmentEntry> enchants = new ArrayList<>();

            for (int i = 1; i <= enchantmentCount; i++) {
                String enchantArg = "enchantment" + i;
                String levelArg = "level" + i;

                try {
                    RegistryEntry.Reference<Enchantment> enchantment =
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
                } catch (IllegalArgumentException e) {
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

    private Item getItem(CommandContext<CommandSource> context) {
        try {
            var itemArg = ItemStackArgumentType.getItemStackArgument(context, "item");
            return itemArg.getItem();
        } catch (Exception e) {
            return null;
        }
    }

    private void optimize(Item item, List<EnchantmentOptimizer.EnchantmentEntry> enchants) {
        try {
            // Create optimizer from current registry
            var registry = mc.getNetworkHandler().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            EnchantmentOptimizer.OptimizationResult result = EnchantmentOptimizer.create(registry).optimize(item, enchants);

            // Display header
            String itemName = item != null ? item.getName().getString() : "Book";
            ChatUtils.info("=== Enchantment Optimization for %s ===", itemName);
            info("Total Cost: (highlight)%d levels(default) (%d XP)", result.totalLevels(), result.totalXp());

            if (result.instructions().isEmpty()) {
                info("No combinations needed - single enchantment only.");
                return;
            }

            info("Steps:");

            // Display steps
            for (int i = 0; i < result.instructions().size(); i++) {
                EnchantmentOptimizer.Instruction instr = result.instructions().get(i);

                MutableText stepText = Text.literal(String.format("  %d. ", i + 1)).formatted(Formatting.GRAY);
                stepText.append(Text.literal("Combine ").formatted(Formatting.GRAY));
                stepText.append(formatItem(instr.left()).copy().formatted(Formatting.YELLOW));
                stepText.append(Text.literal(" with ").formatted(Formatting.GRAY));
                stepText.append(formatItem(instr.right()).copy().formatted(Formatting.AQUA));

                ChatUtils.sendMsg(stepText);

                info("     Cost: (highlight)%d levels(default) (%d XP), Prior Work Penalty: %d",
                    instr.levels(),
                    instr.xp(),
                    instr.priorWorkPenalty()
                );
            }

        } catch (Exception e) {
            error("Failed to optimize enchantments: %s", e.getMessage());
        }
    }

    private Text formatItem(EnchantmentOptimizer.Combination comb) {
        if (comb.item != null) {
            return comb.item.getName();
        }

        if (comb.enchantment != null) {
            String enchName = comb.enchantment.value().description().getString();
            String level = romanNumeral(comb.level);
            return Text.literal(enchName + " " + level + " Book");
        }

        return Text.literal("Combined Item");
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
