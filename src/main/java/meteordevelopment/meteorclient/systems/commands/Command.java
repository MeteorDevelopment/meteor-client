/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public abstract class Command {
    protected static MinecraftClient mc;

    private final String name;
    private final String title;
    private final String description;
    private final List<String> aliases = new ArrayList<>();

    public Command(String name, String description, String... aliases) {
        this.name = name;
        this.title = Utils.nameToTitle(name);
        this.description = description;
        Collections.addAll(this.aliases, aliases);
        mc = MinecraftClient.getInstance();
    }

    // Helper methods to painlessly infer the CommandSource generic type argument
    protected static <T> RequiredArgumentBuilder<CommandSource, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    protected static LiteralArgumentBuilder<CommandSource> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public final void registerTo(CommandDispatcher<CommandSource> dispatcher) {
        register(dispatcher, name);
        for (String alias : aliases) register(dispatcher, alias);
    }

    public void register(CommandDispatcher<CommandSource> dispatcher, String name) {
        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.literal(name);
        builder.then(literal("help").executes(context -> {
            info(description);
            return SINGLE_SUCCESS;
        }));
        build(builder);
        dispatcher.register(builder);
    }

    public abstract void build(LiteralArgumentBuilder<CommandSource> builder);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String toString() {
        return Config.get().prefix.get() + name;
    }

    public String toString(String... args) {
        StringBuilder base = new StringBuilder(toString());
        for (String arg : args)
            base.append(' ').append(arg);

        return base.toString();
    }

    public void info(Text message) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.sendMsg(title, message);
    }

    public void info(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.info(title, message, args);
    }

    public void warning(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.warning(title, message, args);
    }

    public void error(String message, Object... args) {
        ChatUtils.forceNextPrefixClass(getClass());
        ChatUtils.error(title, message, args);
    }
}
