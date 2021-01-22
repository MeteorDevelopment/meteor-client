/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import minegame159.meteorclient.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;

public abstract class Command {
    protected static MinecraftClient mc;

    private final String name;
    private final String description;

    public Command(String name, String description) {
        this.name = name;
        this.description = description;
        mc = MinecraftClient.getInstance();
    }

    // Helper methods to painlessly infer the CommandSource generic type argument
    protected static <T> RequiredArgumentBuilder<CommandSource, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    protected static <T> LiteralArgumentBuilder<CommandSource> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public final void registerTo(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.literal(this.name);
        this.build(builder);
        dispatcher.register(builder);
    }

    public abstract void build(LiteralArgumentBuilder<CommandSource> builder);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    // Generate proper command-string to execute.
    // For example: CommandManager.get(Say.class).toString("raw_message") -> ".say raw_message".
    public String toString() {
        return Config.INSTANCE.getPrefix() + name;
    }

    public String toString(String... args) {
        StringBuilder base = new StringBuilder(toString());
        for (String arg : args)
            base.append(' ').append(arg);

        return base.toString();
    }
}
