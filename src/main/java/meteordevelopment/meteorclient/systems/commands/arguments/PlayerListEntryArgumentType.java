/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.text.LiteralText;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerListEntryArgumentType implements ArgumentType<PlayerListEntry> {

    private static Collection<String> EXAMPLES;

    static {
        if (mc.getNetworkHandler() != null) {
            EXAMPLES = mc.getNetworkHandler().getPlayerList()
                .stream()
                .limit(3)
                .map(playerListEntry -> playerListEntry.getProfile().getName())
                .collect(Collectors.toList());
        }
    }

    private static final DynamicCommandExceptionType NO_SUCH_PLAYER = new DynamicCommandExceptionType(o ->
            new LiteralText("Player list entry with name " + o + " doesn't exist."));

    public static PlayerListEntryArgumentType playerListEntry() {
        return new PlayerListEntryArgumentType();
    }

    public static PlayerListEntry getPlayerListEntry(CommandContext<?> context) {
        return context.getArgument("player", PlayerListEntry.class);
    }

    @Override
    public PlayerListEntry parse(StringReader reader) throws CommandSyntaxException {
        String argument = reader.readString();
        PlayerListEntry playerListEntry = null;

        for (PlayerListEntry p : mc.getNetworkHandler().getPlayerList()) {
            if (p.getProfile().getName().equalsIgnoreCase(argument)) {
                playerListEntry = p;
                break;
            }
        }

        if (playerListEntry == null) throw NO_SUCH_PLAYER.create(argument);
        return playerListEntry;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(mc.getNetworkHandler().getPlayerList().stream().map(playerListEntry -> playerListEntry.getProfile().getName()), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
