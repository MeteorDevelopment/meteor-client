/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.AddonManager;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.translation.TranslationKey;
import meteordevelopment.meteorclient.translation.TranslationManager;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;

import java.util.List;

public abstract class Command {
    protected static CommandBuildContext REGISTRY_ACCESS = Commands.createValidationContext(VanillaRegistries.createLookup());
    protected static final int SINGLE_SUCCESS = com.mojang.brigadier.Command.SINGLE_SUCCESS;
    protected static final Minecraft mc = MeteorClient.mc;

    private final String name;
    private final TranslationKey nameKey;
    private final TranslationKey descriptionKey;
    private final List<String> aliases;

    public Command(String name, String description, String... aliases) {
        this.name = name;
        String ns = AddonManager.namespaceOf(getClass());
        this.nameKey = TranslationKey.of(TranslationManager.commandNameKey(ns, name));
        this.descriptionKey = TranslationKey.of(TranslationManager.commandDescriptionKey(ns, name));
        this.aliases = List.of(aliases);
    }

    // Helper methods to painlessly infer the CommandSource generic type argument
    protected static <T> RequiredArgumentBuilder<ClientSuggestionProvider, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    protected static LiteralArgumentBuilder<ClientSuggestionProvider> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public final void registerTo(CommandDispatcher<ClientSuggestionProvider> dispatcher) {
        register(dispatcher, name);
        for (String alias : aliases) register(dispatcher, alias);
    }

    public void register(CommandDispatcher<ClientSuggestionProvider> dispatcher, String name) {
        LiteralArgumentBuilder<ClientSuggestionProvider> builder = LiteralArgumentBuilder.literal(name);
        build(builder);
        dispatcher.register(builder);
    }

    public abstract void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return descriptionKey.get();
    }

    /** The command title in the current language, resolved lazily. */
    public String getTitle() {
        return nameKey.get();
    }

    public TranslationKey nameKey() {
        return nameKey;
    }

    public TranslationKey descriptionKey() {
        return descriptionKey;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String toString() {
        return Config.get().prefix.get() + name;
    }

    public String toString(String... args) {
        StringBuilder base = new StringBuilder(toString());
        for (String arg : args) base.append(' ').append(arg);
        return base.toString();
    }

    public void info(Component message) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.sendMsg(getTitle(), message);
    }

    public void info(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.infoPrefix(getTitle(), message, args);
    }

    public void warning(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.warningPrefix(getTitle(), message, args);
    }

    public void error(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.errorPrefix(getTitle(), message, args);
    }
}
