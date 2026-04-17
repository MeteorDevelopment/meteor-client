/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.Settings;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class SettingValueArgumentType implements ArgumentType<String> {
    private static final SettingValueArgumentType INSTANCE = new SettingValueArgumentType();

    public static SettingValueArgumentType create() {
        return INSTANCE;
    }

    public static String get(CommandContext<?> context) {
        return context.getArgument("value", String.class);
    }

    private SettingValueArgumentType() {}

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String text = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        return text;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Setting<?> setting;

        try {
            setting = SettingArgumentType.get(context);
        } catch (CommandSyntaxException ignored) {
            return Suggestions.empty();
        }

        return suggest(builder, setting);
    }

    public static <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder, Settings settings) {
        Setting<?> setting;

        try {
            setting = SettingArgumentType.get(context, settings);
        } catch (CommandSyntaxException ignored) {
            return Suggestions.empty();
        }

        return suggest(builder, setting);
    }

    public static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, @NotNull Setting<?> setting) {
        Iterable<Identifier> identifiers = setting.getIdentifierSuggestions();
        if (identifiers != null) {
            return CommandSource.suggestIdentifiers(identifiers, builder);
        }

        return CommandSource.suggestMatching(setting.getSuggestions(), builder);
    }
}
