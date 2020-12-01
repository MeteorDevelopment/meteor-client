/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.player.FakePlayer;
import minegame159.meteorclient.utils.Chat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class FakePlayerCommand extends Command {
    public FakePlayerCommand(){
        super("fakeplayer", "Enchants the currently held item with almost every enchantment (must be in creative)");
    }

    public static final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            if (!ModuleManager.INSTANCE.get(FakePlayer.class).isActive()) Chat.error("The FakePlayer module must be enabled to use this command.");
            else Chat.error("Please enter an argument.");
            return SINGLE_SUCCESS;
        }).then(literal("spawn").executes(context -> {
            ModuleManager.INSTANCE.get(FakePlayer.class).spawnFakePlayer();
            return SINGLE_SUCCESS;
        })).then(literal("clear").executes(context -> {
            ModuleManager.INSTANCE.get(FakePlayer.class).clearFakePlayers();
            return SINGLE_SUCCESS;
        }));
    }
}