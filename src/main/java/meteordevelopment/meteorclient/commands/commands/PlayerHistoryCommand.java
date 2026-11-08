/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.playerhistory.PlayerHistory;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class PlayerHistoryCommand extends Command {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    public PlayerHistoryCommand() {
        super("player-history", "Searches your locally saved player sightings.", "phistory");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(literal("search")
            .then(argument("player", StringArgumentType.word()).executes(context -> {
                String query = StringArgumentType.getString(context, "player");
                var results = PlayerHistory.get().search(query);

                if (results.isEmpty()) {
                    info("No saved sightings found for %s.", query);
                    return SINGLE_SUCCESS;
                }

                info("--- Player sightings for %s (latest %d) ---", query, results.size());
                for (PlayerHistory.Sighting sighting : results) {
                    info("%s: %s at %d, %d, %d in %s; gear: %s",
                        sighting.name,
                        DATE_FORMAT.format(Instant.ofEpochMilli(sighting.timestamp)),
                        sighting.x, sighting.y, sighting.z,
                        sighting.dimension,
                        sighting.gear
                    );
                }
                return SINGLE_SUCCESS;
            }))
        );

        builder.then(literal("list").executes(_ -> {
            info("Player history contains %d sightings. Search with %s search <player>.", PlayerHistory.get().size(), this);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("clear").executes(_ -> {
            PlayerHistory.get().clear();
            info("Cleared player history for this server.");
            return SINGLE_SUCCESS;
        }));

        builder.executes(_ -> {
            info("Use %s search <player>, %s list, or %s clear.", this, this, this);
            return SINGLE_SUCCESS;
        });
    }
}
