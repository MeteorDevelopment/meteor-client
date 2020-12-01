/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.commands.CommandSource;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.utils.Chat;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Friend extends Command {
    public Friend() {
        super("friend", "Manages friends.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add")
                .then(argument("friend", StringArgumentType.greedyString())
                        .executes(context -> {
                            // TODO: Friend argument
                            String friend = context.getArgument("friend", String.class);
                            if (FriendManager.INSTANCE.add(new minegame159.meteorclient.friends.Friend(friend))) {
                                Chat.info("Added (highlight)%s (default)to friends.", friend);
                            } else {
                                Chat.error("That person is already your friend.");
                            }

                            return SINGLE_SUCCESS;
                        }))
                .then(literal("remove").then(argument("friend", StringArgumentType.greedyString())
                        .executes(context -> {
                            // TODO: Friend argument
                            String friend = context.getArgument("friend", String.class);
                            if (FriendManager.INSTANCE.remove(new minegame159.meteorclient.friends.Friend(friend))) {
                                Chat.info("Removed (highlight)%s (default)from friends.", friend);
                            } else {
                                Chat.error("That person is not your friend.");
                            }

                            return SINGLE_SUCCESS;
                        })))
                .then(literal("list").executes(context -> {
                    Chat.info("You have (highlight)%d (default)friends:", FriendManager.INSTANCE.count());

                    for (minegame159.meteorclient.friends.Friend friend : FriendManager.INSTANCE) {
                        Chat.info(" - (highlight)%s", friend.name);
                    }

                    return SINGLE_SUCCESS;
                })));
    }

}
