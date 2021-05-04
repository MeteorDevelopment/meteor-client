/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.player.FakePlayer;
import minegame159.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class FakePlayerCommand extends Command {
    public FakePlayerCommand(){
        super("fake-player", "Manages fake players that you can use for testing.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("spawn").executes(context -> {
            if (active()) FakePlayerManager.add("Meteor on Crack", 36, true);
            return SINGLE_SUCCESS;
        })
                        .then(argument("name", StringArgumentType.word())
                .executes(context -> {
                    if (active()) FakePlayerManager.add(StringArgumentType.getString(context, "name"), 36, true);
                    return SINGLE_SUCCESS;
                })
                .then(argument("health", FloatArgumentType.floatArg(0))
                        .executes(context -> {
                    if (active()) FakePlayerManager.add(StringArgumentType.getString(context, "name"), FloatArgumentType.getFloat(context, "health"), true);
                    return SINGLE_SUCCESS;
                })
                .then(argument("copy-inv", BoolArgumentType.bool())
                        .executes(context -> {
                            if (active()) FakePlayerManager.add(StringArgumentType.getString(context, "name"), FloatArgumentType.getFloat(context, "health"), BoolArgumentType.getBool(context, "copy-inv"));
                            return SINGLE_SUCCESS;
                        })
                )))
        );

        builder.then(literal("clear").executes(context -> {
            if (active()) FakePlayerManager.clear();
            return SINGLE_SUCCESS;
        }));
    }

    private boolean active() {
        if (!Modules.get().isActive(FakePlayer.class)) {
            ChatUtils.moduleError(Modules.get().get(FakePlayer.class),"The FakePlayer module must be enabled to use this command.");
            return false;
        }
        else return true;
    }
}