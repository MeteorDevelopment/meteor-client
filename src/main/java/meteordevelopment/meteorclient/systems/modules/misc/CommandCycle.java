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
        .description("Commands with optional delay. Example: /spawn [5], /home [10], /kill")
        .defaultValue(List.of("/spawn [5]", "/home [10]", "/kill"))
        .build()
    );

    private final Setting<Integer> defaultDelay = sgGeneral.add(new IntSetting.Builder()
        .name("default-delay")
        .description("Default delay in seconds when no [delay] is given")
        .defaultValue(5)
        .min(0)
        .sliderMax(120)
        .build()
    );

    private final Setting<Boolean> loop = sgGeneral.add(new BoolSetting.Builder()
        .name("loop")
        .description("Repeat the list when finished")
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

            int delayTicks = defaultDelay.get() * 20; // seconds → ticks

            Matcher matcher = DELAY_PATTERN.matcher(line);
            if (matcher.find()) {
                try {
                    delayTicks = Integer.parseInt(matcher.group(1)) * 20;
                    line = line.replaceFirst("\\[\\d+]", "").trim(); // remove [number]
                } catch (Exception ignored) {}
            }

            if (!line.startsWith("/")) line = "/" + line;
            parsedCommands.add(new CommandEntry(line, delayTicks));
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

        // Send the command safely
        ChatUtils.sendPlayerMsg(entry.command());

        timer = entry.delay();

        // Go to next command
        currentIndex = (currentIndex + 1) % parsedCommands.size();

        // If loop is disabled & we reached the end → stop module
        if (currentIndex == 0 && !loop.get()) {
            toggle();
        }
    }
}
