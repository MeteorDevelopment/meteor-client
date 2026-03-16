/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EnchantmentLevelArgumentType implements ArgumentType<Integer> {
    private static final SimpleCommandExceptionType INVALID_LEVEL = new SimpleCommandExceptionType(Text.literal("Level must be at least 1"));
    private final String enchantmentArgName;

    public EnchantmentLevelArgumentType(String enchantmentArgName) {
        this.enchantmentArgName = enchantmentArgName;
    }

    public static EnchantmentLevelArgumentType enchantmentLevel(String enchantmentArgName) {
        return new EnchantmentLevelArgumentType(enchantmentArgName);
    }

    @Override
    public Integer parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        int level = reader.readInt();

        if (level < 1) {
            reader.setCursor(start);
            throw INVALID_LEVEL.createWithContext(reader);
        }

        return level;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        try {
            // Try to get the enchantment from the previous argument
            RegistryEntry.Reference<Enchantment> enchantment =
                RegistryEntryReferenceArgumentType.getEnchantment(context, enchantmentArgName);

            int maxLevel = enchantment.value().getMaxLevel();
            String enchantName = enchantment.value().description().getString();

            // Add a tooltip showing the valid range
            String remaining = builder.getRemaining();
            if (!remaining.isEmpty()) {
                try {
                    int typedLevel = Integer.parseInt(remaining);
                    if (typedLevel > maxLevel) {
                        // Show error in suggestions
                        builder.suggest(maxLevel, Text.literal("§c" + enchantName + " max level: " + maxLevel));
                    }

                    return builder.buildFuture();
                } catch (NumberFormatException ignored) {
                    // Command handler highlights invalid input
                }
            }

            // Build suggestions based on max level
            // Suggest 1 through maxLevel
            for (int i = 1; i <= Math.min(maxLevel, 10); i++) {
                builder.suggest(i);
            }

            return builder.buildFuture();
        } catch (Exception e) {
            return Suggestions.empty();
        }
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("1", "2", "3", "4", "5");
    }
}
