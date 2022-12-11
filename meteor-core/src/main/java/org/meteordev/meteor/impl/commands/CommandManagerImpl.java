/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor.impl.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;
import org.jetbrains.annotations.NotNull;
import org.meteordev.meteor.api.commands.Command;
import org.meteordev.meteor.api.commands.CommandManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CommandManagerImpl implements CommandManager {
    public static final CommandManagerImpl INSTANCE = new CommandManagerImpl();

    public final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
    public final CommandSource commandSource = new ClientCommandSource(null, MinecraftClient.getInstance());

    private final Map<String, Command> commands = new HashMap<>();

    @Override
    public void add(Command command) {
        commands.put(command.getName(), command);

        register(command.getName(), command);
        for (String alias : command.getAliases()) register(alias, command);
    }

    @Override
    public Command get(String name) {
        return commands.get(name);
    }

    private void register(String name, Command command) {
        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.literal(name);
        command.build(builder);
        dispatcher.register(builder);
    }

    @NotNull
    @Override
    public Iterator<Command> iterator() {
        return commands.values().iterator();
    }

    public void dispatch(String message) throws CommandSyntaxException {
        ParseResults<CommandSource> results = dispatcher.parse(message, new ClientCommandSource(null, MinecraftClient.getInstance()));
        dispatcher.execute(results);
    }
}
