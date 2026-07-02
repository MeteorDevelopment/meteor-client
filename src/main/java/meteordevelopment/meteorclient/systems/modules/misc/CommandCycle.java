/*
 * This file is part of the Meteor Client distribution[](https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandCycle extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> commands = sgGeneral.add(new StringListSetting.Builder()
        .name("commands")
        .description("Commands with optional delays. Example: /spawn [5] [10] [15], /home [8], /kill")
        .defaultValue(List.of("/spawn [5] [10]", "/home [8]", "/kill"))
        .build()
    );

    private final Setting<Integer> defaultDelay = sgGeneral.add(new IntSetting.Builder()
        .name("default-delay")
        .description("Default delay in seconds if no [delay] is specified")
        .defaultValue(5)
        .min(0)
        .sliderMax(120)
        .build()
    );

    private final Setting<Boolean> loop = sgGeneral.add(new BoolSetting.Builder()
        .name("loop")
        .description("Repeat the sequence when finished")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableOnLeave = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-leave")
        .description("Automatically disable when leaving the server")
        .defaultValue(true)
        .build()
    );

    private int timer = 0;
    private int currentIndex = 0;
    private final List<CommandEntry> parsedCommands = new ArrayList<>();

    private static final Pattern DELAY_PATTERN = Pattern.compile("\\[(\\d+)]");

    private record CommandEntry(String command, int delay) {}

    public CommandCycle() {
        super(Categories.Misc, "command-cycle", "Executes a list of commands in sequence with custom delays.");
    }

    @Override
    public void onActivate() {
        parseCommands();
        currentIndex = 0;
        timer = 0;
    }

    private void parseCommands() {
        parsedCommands.clear();
        for (String line : commands.get()) {
            line = line.trim();
            if (line.isEmpty()) continue;

            int delay = defaultDelay.get() * 20;

            // Find the LAST delay in the line
            Matcher matcher = DELAY_PATTERN.matcher(line);
            while (matcher.find()) {
                try {
                    delay = Integer.parseInt(matcher.group(1)) * 20;
                } catch (Exception ignored) {}
            }

            // Remove all [numbers] from the command
            String command = line.replaceAll("\\[\\d+]", "").trim();
            if (!command.startsWith("/")) command = "/" + command;

            parsedCommands.add(new CommandEntry(command, delay));
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnLeave.get()) toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (parsedCommands.isEmpty() || timer > 0) {
            if (timer > 0) timer--;
            return;
        }

        CommandEntry entry = parsedCommands.get(currentIndex);

        ChatUtils.sendPlayerMsg(entry.command());

        timer = entry.delay();

        currentIndex = (currentIndex + 1) % parsedCommands.size();

        if (currentIndex == 0 && !loop.get()) {
            toggle();
        }
    }
}
