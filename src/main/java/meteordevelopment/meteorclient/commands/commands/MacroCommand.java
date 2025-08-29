/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.MacroArgumentType;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.macros.Macro;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.TimeArgumentType;

import java.util.ArrayList;
import java.util.List;

public class MacroCommand extends Command {
    public MacroCommand() {
        super("macro", "Allows you to execute macros.");

        MeteorClient.EVENT_BUS.subscribe(this);
    }

    List<ScheduledMacro> scheduleQueue = new ArrayList<>();
    List<ScheduledMacro> scheduledMacros = new ArrayList<>();

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder
            .then(literal("clear")
                .executes(context -> {
                    if (scheduleQueue.isEmpty() && scheduledMacros.isEmpty()) {
                        error("No macros are currently scheduled.");
                        return SINGLE_SUCCESS;
                    }

                    clearAll();
                    info("Cleared all scheduled macros.");

                    return SINGLE_SUCCESS;
                })
            )
            .then(argument("macro", MacroArgumentType.create())
                .executes(context -> {
                    Macro macro = MacroArgumentType.get(context);
                    scheduleQueue.add(new ScheduledMacro(1, macro));

                    return SINGLE_SUCCESS;
                })
                .then(literal("clear")
                    .executes(context -> {
                        Macro macro = MacroArgumentType.get(context);

                        if (!isScheduled(macro)) {
                            error("This module is not currently scheduled.");
                            return SINGLE_SUCCESS;
                        };

                        clear(macro);
                        info("Cleared scheduled macro.");
                        return SINGLE_SUCCESS;
                    })
                )
                .then(argument("delay", TimeArgumentType.time(1))
                    .executes(context -> {
                        Macro macro = MacroArgumentType.get(context);
                        scheduleQueue.add(new ScheduledMacro(IntegerArgumentType.getInteger(context, "delay"), macro));

                        return SINGLE_SUCCESS;
                    })
                )
            )
        ;
    }

    public void clearAll() {
        scheduleQueue.clear();
        scheduledMacros.clear();
    }

    public boolean isScheduled(Macro macro) {
        return scheduleQueue.stream().anyMatch(element -> element.macro == macro) ||
            scheduledMacros.stream().noneMatch(element -> element.macro == macro);
    }

    public void clear(Macro macro) {
        scheduleQueue.removeIf(scheduledMacro -> scheduledMacro.macro == macro);
        scheduledMacros.removeIf(scheduledMacro -> scheduledMacro.macro == macro);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!scheduleQueue.isEmpty()) {
            scheduledMacros.addAll(scheduleQueue);
            scheduleQueue.clear();
        }

        if (!scheduledMacros.isEmpty()) {
            tickMacros();
        }
    }

    private void tickMacros() {
        scheduledMacros.removeIf(ScheduledMacro::tick);
    }
}

class ScheduledMacro {
    public int delay;
    public Macro macro;


    public ScheduledMacro (int tickDelay, Macro scheduledMacro) {
        delay = tickDelay;
        macro = scheduledMacro;
    }

    public boolean tick() {
       delay--;
       if (delay > 0) { return false; }

       run();
       return true;
    }

    private void run() {
        if (MeteorClient.mc.player == null) { return; }

        macro.onAction();
    }
}
