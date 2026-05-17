/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerListEntryArgumentType implements ArgumentType<PlayerInfo> {
    private static final PlayerListEntryArgumentType INSTANCE = new PlayerListEntryArgumentType();
    private static final DynamicCommandExceptionType NO_SUCH_PLAYER = new DynamicCommandExceptionType(name -> Component.literal("Player list entry with name " + name + " doesn't exist."));

    private static final Collection<String> EXAMPLES = List.of("seasnail8169", "MineGame159");

    public static PlayerListEntryArgumentType create() {
        return INSTANCE;
    }

    public static PlayerInfo get(CommandContext<?> context) {
        return context.getArgument("player", PlayerInfo.class);
    }

    private PlayerListEntryArgumentType() {
    }

    @Override
    public PlayerInfo parse(StringReader reader) throws CommandSyntaxException {
        String argument = reader.readString();
        PlayerInfo playerListEntry = null;

        for (PlayerInfo p : mc.getConnection().getOnlinePlayers()) {
            if (p.getProfile().name().equalsIgnoreCase(argument)) {
                playerListEntry = p;
                break;
            }
        }
        if (playerListEntry == null) throw NO_SUCH_PLAYER.create(argument);

        return playerListEntry;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(mc.getConnection().getOnlinePlayers().stream().map(playerListEntry -> playerListEntry.getProfile().name()), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
