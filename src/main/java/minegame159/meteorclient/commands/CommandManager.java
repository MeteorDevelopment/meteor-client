/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import minegame159.meteorclient.commands.commands.*;
import minegame159.meteorclient.commands.commands.swarm.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CommandManager {
    private static final CommandDispatcher<CommandSource> DISPATCHER = new CommandDispatcher<>();
    private static final CommandSource COMMAND_SOURCE = new ChatCommandSource(MinecraftClient.getInstance());
    private static final List<Command> commands = new ArrayList<>();
    private static final Map<Class<? extends Command>, Command> commandInstances = new HashMap<>();

    public static void init() {
        addCommand(new Baritone());
        addCommand(new Bind());
        addCommand(new VClip());
        addCommand(new HClip());
        addCommand(new ClearChat());
        addCommand(new Dismount());
        addCommand(new Drop());
        addCommand(new Enchant());
        addCommand(new FakePlayerCommand());
        addCommand(new Friend());
        addCommand(new Help());
        addCommand(new Ignore());
        addCommand(new NBT());
        addCommand(new Panic());
        addCommand(new Peek());
        addCommand(new Profile());
        addCommand(new Reload());
        addCommand(new Reset());
        addCommand(new Say());
        addCommand(new SwarmModuleToggle());
        addCommand(new SwarmQueen());
        addCommand(new SwarmSlave());
        addCommand(new SwarmEscape());
        addCommand(new SwarmGoto());
        addCommand(new SwarmFollow());
        addCommand(new SwarmScatter());
        addCommand(new SwarmMine());
        addCommand(new SwarmInfinityMiner());
        addCommand(new SwarmRelease());
        addCommand(new SwarmStop());
        addCommand(new SwarmCloseConnections());
        addCommand(new Toggle());
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

    private static void addCommand(Command command) {
        command.registerTo(DISPATCHER);
        commands.add(command);
        commandInstances.put(command.getClass(), command);
    }

    public static int getCount() {
        return commands.size();
    }

    public static void forEach(Consumer<Command> consumer) {
        commands.forEach(consumer);
    }

    public static <T extends Command> T get(Class<T> klass) {
        return (T) commandInstances.get(klass);
    }
}
