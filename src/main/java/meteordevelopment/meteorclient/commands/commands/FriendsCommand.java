/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.FriendArgumentType;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.commands.arguments.ClientArgumentType;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;

public class FriendsCommand extends Command {
    public FriendsCommand() {
        super("friends", "Manages friends.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        // Add friend
        builder.then(literal("add")
            .then(argument("player", PlayerListEntryArgumentType.create())
                .executes(context -> {
                    GameProfile profile = PlayerListEntryArgumentType.get(context).getProfile();
                    Friend friend = new Friend(profile.name(), profile.id());

                    if (Friends.get().add(friend)) {
                        ChatUtils.sendMsg(friend.hashCode(), Formatting.GRAY, "Added (highlight)%s (default) to friends.".formatted(friend.getName()));
                    } else error("Already friends with that player.");

                    return SINGLE_SUCCESS;
                })
            )
        );

        // Remove friend
        builder.then(literal("remove")
            .then(argument("friend", FriendArgumentType.create())
                .executes(context -> {
                    Friend friend = FriendArgumentType.get(context);
                    if (friend == null) {
                        error("Not friends with that player.");
                        return SINGLE_SUCCESS;
                    }

                    if (Friends.get().remove(friend)) {
                        ChatUtils.sendMsg(friend.hashCode(), Formatting.GRAY, "Removed (highlight)%s (default) from friends.".formatted(friend.getName()));
                    } else error("Failed to remove that friend.");

                    return SINGLE_SUCCESS;
                })
            )
        );

        // friend sync
        builder.then(literal("sync")
            .executes(context -> {
                ChatUtils.sendMsg(0, Formatting.GRAY, "Supported clients: mio, wurst, all");
                return SINGLE_SUCCESS;
            })
                .then(argument("client", ClientArgumentType.create())
                    .suggests((context, suggestionsBuilder) -> {
                        ClientArgumentType clientArg = ClientArgumentType.create();
                        for (String client : clientArg.getExamples()) {
                            suggestionsBuilder.suggest(client);
                        }
                        return suggestionsBuilder.buildFuture();
                    })
                    .executes(context -> {
                        String client = ClientArgumentType.get(context, "client");
                        int added = 0;

                        try {
                            switch (client) {
                                case "mio" -> added = Friends.get().importFromMio();
                                case "wurst" -> added = Friends.get().importFromWurst();
                            }

                            if (added == -1) {
                                error(client + " friend file not found.");
                            } else {
                                ChatUtils.sendMsg(0, Formatting.GRAY, "Imported " + added + " friends from " + client + ".");
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            error("Failed to sync " + client + " friends.");
                        }

                        return SINGLE_SUCCESS;
                    })
                )
        );

        // List friends
        builder.then(literal("list").executes(context -> {
                info("--- Friends ((highlight)%s(default)) ---", Friends.get().count());
                Friends.get().forEach(friend -> ChatUtils.info("(highlight)%s".formatted(friend.getName())));
                return SINGLE_SUCCESS;
            })
        );
    }
}
