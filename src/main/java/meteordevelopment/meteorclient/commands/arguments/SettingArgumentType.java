/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class SettingArgumentType implements ArgumentType<String> {
    private static final SettingArgumentType INSTANCE = new SettingArgumentType();
    private static final DynamicCommandExceptionType NO_SUCH_SETTING = new DynamicCommandExceptionType(name -> Text.literal("No such setting '" + name + "'."));

    public static SettingArgumentType create() {
        return INSTANCE;
    }

    public static Setting<?> get(CommandContext<?> context) throws CommandSyntaxException {
        Module module = context.getArgument("module", Module.class);

        return get(context, module.settings);
    }

    public static Setting<?> get(CommandContext<?> context, Settings settings) throws CommandSyntaxException {
        String settingName = context.getArgument("setting", String.class);

        Setting<?> setting = settings.get(settingName);
        if (setting == null) throw NO_SUCH_SETTING.create(settingName);

        return setting;
    }

    private SettingArgumentType() {}

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return listSuggestions(builder, context.getArgument("module", Module.class).settings);
    }

    public static CompletableFuture<Suggestions> listSuggestions(SuggestionsBuilder builder, Settings settings) {
        Stream<String> stream = Streams.stream(settings.iterator())
            .flatMap(sg -> Streams.stream(sg.iterator()))
            .map(setting -> setting.name);

        return CommandSource.suggestMatching(stream, builder);
    }
}
