/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.FriendArgumentType;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.misc.text.MessageBuilder;
import net.minecraft.command.CommandSource;

public class FriendsCommand extends Command {
    public FriendsCommand() {
        super("friends");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add")
            .then(argument("player", PlayerListEntryArgumentType.create())
                .executes(context -> {
                    GameProfile profile = PlayerListEntryArgumentType.get(context).getProfile();
                    Friend friend = new Friend(profile.name(), profile.id());

                    if (Friends.get().add(friend)) {
                        this.info("added", friend.getName()).setId(friend.hashCode()).send();
                    }
                    else error("already_friends");

                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("remove")
            .then(argument("friend", FriendArgumentType.create())
                .executes(context -> {
                    Friend friend = FriendArgumentType.get(context);
                    if (friend == null) {
                        this.error("not_friends").send();
                        return SINGLE_SUCCESS;
                    }

                    if (Friends.get().remove(friend)) {
                        this.info("removed", friend.getName()).setId(friend.hashCode()).send();
                    }
                    else this.error("failed").send();

                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("list").executes(context -> {
            this.info("friends", MessageBuilder.highlight(Friends.get().count())).send();
            Friends.get().forEach(friend -> this.info(MessageBuilder.highlight(friend.getName())).send());
            return SINGLE_SUCCESS;
        }));
    }
}
