/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.PostInit;
import net.minecraft.command.CommandSource;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Commands {
    public static final CommandDispatcher<CommandSource> DISPATCHER = new CommandDispatcher<>();
    public static final List<Command> COMMANDS = new ArrayList<>();

    @PostInit(dependencies = PathManagers.class)
    public static void init() {
        add(new VClipCommand());
        add(new HClipCommand());
        add(new DismountCommand());
        add(new DisconnectCommand());
        add(new DamageCommand());
        add(new DropCommand());
        add(new EnchantCommand());
        add(new FakePlayerCommand());
        add(new FriendsCommand());
        add(new CommandsCommand());
        add(new InventoryCommand());
        add(new NbtCommand());
        add(new NotebotCommand());
        add(new PeekCommand());
        add(new EnderChestCommand());
        add(new ProfilesCommand());
        add(new ReloadCommand());
        add(new ResetCommand());
        add(new SayCommand());
        add(new ServerCommand());
        add(new SwarmCommand());
        add(new ToggleCommand());
        add(new SettingCommand());
        add(new SpectateCommand());
        add(new GamemodeCommand());
        add(new SaveMapCommand());
        add(new MacroCommand());
        add(new ModulesCommand());
        add(new BindsCommand());
        add(new GiveCommand());
        add(new NameHistoryCommand());
        add(new BindCommand());
        add(new FovCommand());
        add(new RotationCommand());
        add(new WaypointCommand());
        add(new InputCommand());
        add(new WaspCommand());
        add(new LocateCommand());

        COMMANDS.sort(Comparator.comparing(Command::getName));
    }

    /**
     * Registers a package to scan for commands.
     * This should be called by addons during their initialization.
     */
    public static void registerCommandPackage(String packageName) {
        SCAN_PACKAGES.add(packageName);
    }

    /**
     * Registers a command to be loaded conditionally.
     *
     * @param commandClass The class of the command to register
     * @param condition    A supplier that returns true if the command should be loaded
     */
    public static void registerConditionalCommand(Class<? extends Command> commandClass, Supplier<Boolean> condition) {
        CONDITIONAL_COMMANDS.put(commandClass.getName(), condition);
    }

    public static void loadCommands() {
        try {
            long startTime = java.lang.System.currentTimeMillis();
            int totalCount = 0;
            int skippedCount = 0;

            ConfigurationBuilder config = new ConfigurationBuilder()
                .forPackages(SCAN_PACKAGES.toArray(new String[0]))
                .setScanners(Scanners.SubTypes)
                .setParallel(true)
                .setExpandSuperTypes(false);

            Reflections reflections = new Reflections(config);
            Set<Class<? extends Command>> commandClasses = reflections.getSubTypesOf(Command.class);

            commandClasses = commandClasses.stream()
                .filter(commandClass -> SCAN_PACKAGES.stream().anyMatch(pkg -> commandClass.getName().startsWith(pkg)))
                .collect(Collectors.toSet());

            Map<String, List<Class<? extends Command>>> commandsByPackage = new HashMap<>();
            for (Class<? extends Command> commandClass : commandClasses) {
                String packageName = commandClass.getPackage().getName();
                commandsByPackage.computeIfAbsent(packageName, k -> new ArrayList<>()).add(commandClass);
            }

            for (Class<? extends Command> commandClass : commandClasses) {
                String className = commandClass.getName();
                if (CONDITIONAL_COMMANDS.containsKey(className) && !CONDITIONAL_COMMANDS.get(className).get()) {
                    MeteorClient.LOG.info("Skipping conditional command: {}", className);
                    skippedCount++;
                    continue;
                }

                try {
                    if (Modifier.isAbstract(commandClass.getModifiers()) || commandClass.isInterface()) continue;

                    Constructor<? extends Command> constructor = commandClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    Command command = constructor.newInstance();
                    add(command);
                    totalCount++;
                } catch (NoSuchMethodException ignored) {
                    MeteorClient.LOG.error("Command {} does not have a no-args constructor", commandClass.getName());
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    MeteorClient.LOG.error("Failed to load command: {}", commandClass.getName(), e);
                }
            }

            long endTime = java.lang.System.currentTimeMillis();

            for (Entry<String, List<Class<? extends Command>>> entry : commandsByPackage.entrySet()) {
                String packageName = entry.getKey();
                int count = entry.getValue().size();
                MeteorClient.LOG.info("Found {} commands in {}", count, packageName);
            }

            MeteorClient.LOG.info("Loaded {} commands ({} skipped) in {}ms", totalCount, skippedCount, endTime - startTime);
        } catch (Exception e) {
            MeteorClient.LOG.error("Failed to load commands", e);
        }
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
