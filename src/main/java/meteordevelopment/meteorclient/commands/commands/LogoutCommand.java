/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.LogoutSpots;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class LogoutCommand extends Command {
    public LogoutCommand() {
        super("logout-spots", "Manage logout spots - clear all spots or remove specific players", "logout");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        LogoutSpots logoutSpots = Modules.get().get(LogoutSpots.class);

        builder.then(literal("clear")
            .executes(_ -> {
                logoutSpots.clearLogoutSpots();
                
                ChatUtils.info("Cleared all logout spots");
                return SINGLE_SUCCESS;
            })
        );

        builder.then(literal("remove")
            .then(argument("name", StringArgumentType.word())
                .executes(context -> {
                    String playerName = StringArgumentType.getString(context, "name");
                    boolean removed = logoutSpots.removeLogoutSpot(playerName);
                    
                    if (removed) {
                        ChatUtils.info("Removed logout spot for player: " + playerName);
                    } else {
                        ChatUtils.error("No logout spot found for player: " + playerName);
                    }
                    return SINGLE_SUCCESS;
                })
            )
        );


    }
}
