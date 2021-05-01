/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PlayerArgumentType implements ArgumentType<PlayerEntity> {

    private static Collection<String> EXAMPLES;

    static {
        if (MinecraftClient.getInstance().world != null) {
           EXAMPLES = MinecraftClient.getInstance().world.getPlayers()
                    .stream()
                    .limit(3)
                    .map(playerEntity -> playerEntity.getGameProfile().getName())
                    .collect(Collectors.toList());
        }
    }

    private static final DynamicCommandExceptionType NO_SUCH_PLAYER = new DynamicCommandExceptionType(o ->
            new LiteralText("Player with name " + o + " doesn't exist."));

    public static PlayerArgumentType player() {
        return new PlayerArgumentType();
    }

    public static PlayerEntity getPlayer(CommandContext<?> context) {
        return context.getArgument("player", PlayerEntity.class);
    }

    @Override
    public PlayerEntity parse(StringReader reader) throws CommandSyntaxException {
        String argument = reader.readString();
        PlayerEntity playerEntity = null;
        for (PlayerEntity p : MinecraftClient.getInstance().world.getPlayers()) {
            if (p.getGameProfile().getName().equalsIgnoreCase(argument)) {
                playerEntity = p;
                break;
            }
        }
        if (playerEntity == null) throw NO_SUCH_PLAYER.create(argument);
        return playerEntity;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(MinecraftClient.getInstance().world.getPlayers().stream().map(playerEntity -> playerEntity.getGameProfile().getName()), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
