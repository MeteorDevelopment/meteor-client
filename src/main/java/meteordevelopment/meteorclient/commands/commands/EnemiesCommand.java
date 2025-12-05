/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.EnemyArgumentType;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.systems.targeting.SavedPlayer;
import meteordevelopment.meteorclient.systems.targeting.Targeting;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;

// TODO: functionality duplication

public class EnemiesCommand extends Command {
    public EnemiesCommand() {
        super("enemies", "Manages enemies.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add")
            .then(argument("player", PlayerListEntryArgumentType.create())
                .executes(context -> {
                    GameProfile profile = PlayerListEntryArgumentType.get(context).getProfile();
                    SavedPlayer enemy = new SavedPlayer(profile.name(), profile.id());

                    if (Targeting.get().addEnemy(enemy)) {
                        ChatUtils.sendMsg(enemy.hashCode(), Formatting.GRAY, "Added (highlight)%s (default)to enemies.".formatted(enemy.getName()));
                    }
                    else error("Already enemies with that player.");

                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("remove")
            .then(argument("enemy", EnemyArgumentType.create())
                .executes(context -> {
                    SavedPlayer enemy = EnemyArgumentType.get(context);
                    if (enemy == null) {
                        error("Not enemies with that player.");
                        return SINGLE_SUCCESS;
                    }

                    if (Targeting.get().removeEnemy(enemy)) {
                        ChatUtils.sendMsg(enemy.hashCode(), Formatting.GRAY, "Removed (highlight)%s (default)from enemies.".formatted(enemy.getName()));
                    }
                    else error("Failed to remove that enemy.");

                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("list").executes(context -> {
                info("--- Targeting ((highlight)%s(default)) ---", Targeting.get().countEnemies());
                Targeting.get().getEnemies().forEach(enemy -> ChatUtils.info("(highlight)%s".formatted(enemy.getName())));
                return SINGLE_SUCCESS;
            })
        );
    }
}
