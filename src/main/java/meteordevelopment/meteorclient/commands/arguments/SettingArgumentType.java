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
import meteordevelopment.meteorclient.utils.commands.ArgumentFunction;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class SettingArgumentType implements ArgumentType<SettingArgumentType.SettingArgument> {
    private static final DynamicCommandExceptionType NO_SUCH_SETTING = new DynamicCommandExceptionType(name -> Text.literal("No such setting '" + name + "'."));

    private final ArgumentFunction<?, Settings> settingsArgumentFunction;

    private SettingArgumentType(ArgumentFunction<?, Settings> settingsArgumentFunction) {
        this.settingsArgumentFunction = settingsArgumentFunction;
    }

    public static <S> SettingArgumentType create(ArgumentFunction<S, Settings> settingsArgumentFunction) {
        return new SettingArgumentType(settingsArgumentFunction);
    }

    public static <S> SettingArgumentType createModule(ArgumentFunction<S, Module> moduleArgumentFunction) {
        return new SettingArgumentType(moduleArgumentFunction.andThen(module -> module.settings));
    }

    public static <S> Setting<?> get(CommandContext<S> context) throws CommandSyntaxException {
        return context.getArgument("setting", SettingArgument.class).getSetting(context);
    }

    public static <S> Setting<?> get(CommandContext<S> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, SettingArgument.class).getSetting(context);
    }

    @Override
    public SettingArgument parse(StringReader reader) throws CommandSyntaxException {
        return new SettingArgument(reader.readString(), this.settingsArgumentFunction);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        try {
            return CommandSource.suggestMatching(Streams.stream(this.settingsArgumentFunction.uncheckedApply(context).iterator())
                .flatMap(settings -> Streams.stream(settings.iterator()))
                .filter(Setting::isVisible)
                .map(setting -> setting.name), builder);
        } catch (CommandSyntaxException e) {
            return Suggestions.empty();
        }
    }

    public record SettingArgument(String settingName, ArgumentFunction<?, Settings> settingsArgumentFunction) {
        public <S> Setting<?> getSetting(CommandContext<S> context) throws CommandSyntaxException {
            Settings settings = this.settingsArgumentFunction().uncheckedApply(context);
            @Nullable Setting<?> setting = settings.get(this.settingName());
            if (setting == null) {
                throw NO_SUCH_SETTING.create(this.settingName());
            }
            return setting;
        }
    }
}
