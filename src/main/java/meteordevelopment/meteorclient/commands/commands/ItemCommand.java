/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.Item;

import java.util.ArrayList;

public class ItemCommand extends Command {
    public ItemCommand() {
        super("item", "Manages item related modules.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder

            .then(literal("highlight")
                .then(literal("add")
                    .then(argument("item", ItemStackArgumentType.itemStack(REGISTRY_ACCESS))
                    .executes(context -> {
                        MeteorClient.LOG.info(context.getArgument("item", ItemStackArgument.class).getItem().toString());
                        return SINGLE_SUCCESS;
                    })
                ))
                .then(literal("remove")
                    .executes(context -> {
                        return SINGLE_SUCCESS;
                    })
                )
            );
    }
}
