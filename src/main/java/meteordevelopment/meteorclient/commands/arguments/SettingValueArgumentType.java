/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.commands.ArgumentFunction;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class SettingValueArgumentType implements ArgumentType<String> {
    private final ArgumentFunction<?, Setting<?>> settingArgumentFunction;

    private SettingValueArgumentType(ArgumentFunction<?, Setting<?>> settingArgumentFunction) {
        this.settingArgumentFunction = settingArgumentFunction;
    }

    public static <S> SettingValueArgumentType create(ArgumentFunction<S, Setting<?>> settingArgumentFunction) {
        return new SettingValueArgumentType(settingArgumentFunction);
    }

    public static <S> String get(CommandContext<S> context) {
        return context.getArgument("value", String.class);
    }

    public static <S> String get(CommandContext<S> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String settingValue = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        return settingValue;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        try {
            Setting<?> setting = this.settingArgumentFunction.uncheckedApply(context);

            Iterable<Identifier> identifiers = setting.getIdentifierSuggestions();
            if (identifiers != null) {
                return CommandSource.suggestIdentifiers(identifiers, builder);
            }

            return CommandSource.suggestMatching(setting.getSuggestions(), builder);
        } catch (CommandSyntaxException e) {
            return Suggestions.empty();
        }
    }
}
