/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import minegame159.meteorclient.commands.commands.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;

public class CommandManager {
    private static final CommandDispatcher<CommandSource> DISPATCHER = new CommandDispatcher<>();
    private static final CommandSource COMMAND_SOURCE = new ChatCommandSource(MinecraftClient.getInstance());

    public static void init() {
        addCommand(new Bind());
        addCommand(new ClearChat());
        addCommand(new Modules());
        addCommand(new ResetBind());
        addCommand(new Settings());
        addCommand(new Enchant());
        addCommand(new Reset());
        addCommand(new Panic());
        addCommand(new ResetAll());
        addCommand(new Baritone());
        addCommand(new Reload());
        addCommand(new Dismount());
        addCommand(new Say());
        addCommand(new Ignore());
        addCommand(new Drop());
        addCommand(new HClip());
        addCommand(new VClip());
        addCommand(new Friend());
        addCommand(new ResetGui());
        addCommand(new Peek());
        addCommand(new FakePlayerCommand());
    }

    public static void dispatch(String message) throws CommandSyntaxException {
        dispatch(message, new ChatCommandSource(MinecraftClient.getInstance()));
    }

    public static void dispatch(String message, CommandSource source) throws CommandSyntaxException {
        ParseResults<CommandSource> results = DISPATCHER.parse(message, source);
        // `results` carries information about whether or not the command failed to parse, which path was took, etc.
        // it might be useful to inspect later, before executing.
        CommandManager.DISPATCHER.execute(results);
    }

    private static void addCommand(Command command) {
        command.registerTo(DISPATCHER);
    }

    public static CommandDispatcher<CommandSource> getDispatcher() {
        return DISPATCHER;
    }

    public static CommandSource getCommandSource() {
        return COMMAND_SOURCE;
    }

    private final static class ChatCommandSource extends ClientCommandSource {
        public ChatCommandSource(MinecraftClient client) {
            super(null, client);
        }
    }
}
