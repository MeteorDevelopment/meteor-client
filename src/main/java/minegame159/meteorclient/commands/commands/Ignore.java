/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

//Created by squidoodly 01/07/2020

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.Chat;
import net.minecraft.command.CommandSource;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Ignore extends Command {
    public Ignore() {
        super("ignore", "Lets you ignore messages from specific players.");
    }

    public static List<String> ignoredPlayers = new ArrayList<>();

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("username", StringArgumentType.string()).executes(context -> {
            String username = context.getArgument("username", String.class);

            if (ignoredPlayers.remove(username)) {
                Chat.info("Removed (highlight)%s (default)from list of ignored people.", username);
            } else {
                ignoredPlayers.add(username);
                Chat.info("Added (highlight)%s (default)to list of ignored people.", username);
            }

            return SINGLE_SUCCESS;
        })).executes(context -> {
            Chat.info("Ignoring (highlight)%d (default)people:", ignoredPlayers.size());
            for (String player : ignoredPlayers) {
                Chat.info("- (highlight)%s", player);
            }

            return SINGLE_SUCCESS;
        });
    }

    public static void load() {
        File file = new File(MeteorClient.FOLDER, "ignored_players.txt");
        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));

                String line;
                while ((line = reader.readLine()) != null) Ignore.ignoredPlayers.add(line.split(" ")[0]);

                reader.close();
            } catch (IOException ignored) {
                Ignore.ignoredPlayers = new ArrayList<>();
            }
        }
    }

    public static void save() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(MeteorClient.FOLDER, "ignored_players.txt")));
            for (String name : Ignore.ignoredPlayers) {
                writer.write(name);
                writer.write(" OwO\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
