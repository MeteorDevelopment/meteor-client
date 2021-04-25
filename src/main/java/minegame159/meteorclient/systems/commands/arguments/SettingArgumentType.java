/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.arguments;

import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.command.CommandSource;
import net.minecraft.text.LiteralText;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class SettingArgumentType implements ArgumentType<String> {
    private static final DynamicCommandExceptionType NO_SUCH_SETTING = new DynamicCommandExceptionType(o -> new LiteralText("No such setting '" + o + "'."));

    public static SettingArgumentType setting() {
        return new SettingArgumentType();
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

    public static Setting<?> getSetting(CommandContext<?> context) throws CommandSyntaxException {
        Module module = context.getArgument("module", Module.class);
        String settingName = context.getArgument("setting", String.class);

        Setting<?> setting = module.settings.get(settingName);
        if (setting == null) throw NO_SUCH_SETTING.create(settingName);

        return setting;
    }
}
