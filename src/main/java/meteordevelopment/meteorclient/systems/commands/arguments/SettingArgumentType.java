/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.arguments;

import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class SettingArgumentType implements ArgumentType<String> {
    private static final DynamicCommandExceptionType NO_SUCH_SETTING = new DynamicCommandExceptionType(name -> Text.literal("No such setting '" + name + "'."));

    public static SettingArgumentType create() {
        return new SettingArgumentType();
    }

    public static Setting<?> get(CommandContext<?> context) throws CommandSyntaxException {
        Module module = context.getArgument("module", Module.class);
        String settingName = context.getArgument("setting", String.class);

        Setting<?> setting = module.settings.get(settingName);
        if (setting == null) throw NO_SUCH_SETTING.create(settingName);

        return setting;
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Stream<String> stream = Streams.stream(context.getArgument("module", Module.class).settings.iterator())
                .flatMap(settings -> Streams.stream(settings.iterator()))
                .map(setting -> setting.name);

        return CommandSource.suggestMatching(stream, builder);
    }
}
