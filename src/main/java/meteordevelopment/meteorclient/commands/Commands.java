/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.PostInit;
import net.minecraft.command.CommandSource;

import org.reflections.Reflections;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Commands {
    public static final CommandDispatcher<CommandSource> DISPATCHER = new CommandDispatcher<>();
    public static final List<Command> COMMANDS = new ArrayList<>();

    @PostInit(dependencies = PathManagers.class)
    public static void init() {
        Reflections reflections = new Reflections("meteordevelopment.meteorclient.commands.commands");
        
        Set<Class<? extends Command>> commandClasses = reflections.getSubTypesOf(Command.class);

        for (Class<? extends Command> cmdClass : commandClasses) {
            try {
                add(cmdClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        COMMANDS.sort(Comparator.comparing(Command::getName));
    }

    public static void add(Command command) {
        COMMANDS.removeIf(existing -> existing.getName().equals(command.getName()));
        command.registerTo(DISPATCHER);
        COMMANDS.add(command);
    }

    public static void dispatch(String message) throws CommandSyntaxException {
        DISPATCHER.execute(message, mc.getNetworkHandler().getCommandSource());
    }

    public static Command get(String name) {
        for (Command command : COMMANDS) {
            if (command.getName().equals(name)) {
                return command;
            }
        }

        return null;
    }
}
