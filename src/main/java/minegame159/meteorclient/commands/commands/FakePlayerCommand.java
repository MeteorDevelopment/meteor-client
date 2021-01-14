/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.player.FakePlayer;
import minegame159.meteorclient.utils.player.Chat;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class FakePlayerCommand extends Command {
    public FakePlayerCommand(){
        super("fake-player", "Manages fake players that you can use for testing.");
    }

    public static FakePlayer fakePlayer = ModuleManager.INSTANCE.get(FakePlayer.class);

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("spawn").executes(context -> {
            if (active()) fakePlayer.spawnFakePlayer(fakePlayer.getName(), fakePlayer.copyInv(), fakePlayer.setGlowing(), fakePlayer.getHealth());
            return SINGLE_SUCCESS;
        })).then(literal("remove").then(argument("id", IntegerArgumentType.integer()).executes(context -> {
            int id = context.getArgument("id", Integer.class);
            if (active()) fakePlayer.removeFakePlayer(id);
            return SINGLE_SUCCESS;
        }))).then(literal("clear").executes(context -> {
            if (active()) fakePlayer.clearFakePlayers(true);
            return SINGLE_SUCCESS;
        }));
    }

    private boolean active() {
        if (!ModuleManager.INSTANCE.get(FakePlayer.class).isActive()) {
            Chat.error("The FakePlayer module must be enabled to use this command.");
            return false;
        }
        else return true;
    }
}