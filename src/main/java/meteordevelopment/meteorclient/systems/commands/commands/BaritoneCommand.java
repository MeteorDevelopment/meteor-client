/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.command.CommandSource;

public class BaritoneCommand extends Command {
    public BaritoneCommand() {
        super("baritone", "Executes baritone commands.", "b");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // TODO: Baritone
        /*builder.then(argument("command", StringArgumentType.greedyString())
                .executes(context -> {
                    String command = context.getArgument("command", String.class);
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(command);
                    return SINGLE_SUCCESS;
                }));*/
    }
}
