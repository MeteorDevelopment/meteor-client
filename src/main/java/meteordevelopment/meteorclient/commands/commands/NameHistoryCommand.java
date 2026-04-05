/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.*;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class NameHistoryCommand extends Command {
    public NameHistoryCommand() {
        super("name-history", "Provides a list of a players previous names from the laby.net api.", "history", "names");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(argument("player", PlayerListEntryArgumentType.create()).executes(context -> {
            MeteorExecutor.execute(() -> {
                PlayerInfo lookUpTarget = PlayerListEntryArgumentType.get(context);
                UUID uuid = lookUpTarget.getProfile().id();

                NameHistory history = Http.get("https://laby.net/api/v2/user/" + uuid + "/get-profile")
                    .exceptionHandler(e -> error("There was an error fetching that users name history."))
                    .sendJson(NameHistory.class);

                if (history == null) {
                    return;
                } else if (history.username_history == null || history.username_history.length == 0) {
                    error("There was an error fetching that users name history.");
                }

                String name = lookUpTarget.getProfile().name();
                MutableComponent initial = Component.literal(name);
                initial.append(Component.literal(name.endsWith("s") ? "'" : "'s"));

                Color nameColor = PlayerUtils.getPlayerColor(mc.level.getPlayerByUUID(uuid), Utils.WHITE);

                initial.setStyle(initial.getStyle()
                    .withColor(TextColor.fromRgb(nameColor.getPacked()))
                    .withClickEvent(new ClickEvent.OpenUrl(
                            URI.create("https://laby.net/@" + name)
                        )
                    )
                    .withHoverEvent(new HoverEvent.ShowText(
                        Component.literal("View on laby.net")
                            .withStyle(ChatFormatting.YELLOW)
                            .withStyle(ChatFormatting.ITALIC)
                    ))
                );

                info(initial.append(Component.literal(" Username History:").withStyle(ChatFormatting.GRAY)));

                for (Name entry : history.username_history) {
                    MutableComponent nameText = Component.literal(entry.name);
                    nameText.withStyle(ChatFormatting.AQUA);

                    if (entry.changed_at != null && entry.changed_at.getTime() != 0) {
                        MutableComponent changed = Component.literal("Changed at: ");
                        changed.withStyle(ChatFormatting.GRAY);

                        DateFormat formatter = new SimpleDateFormat("hh:mm:ss, dd/MM/yyyy");
                        changed.append(Component.literal(formatter.format(entry.changed_at)).withStyle(ChatFormatting.WHITE));

                        nameText.setStyle(nameText.getStyle().withHoverEvent(new HoverEvent.ShowText(changed)));
                    }

                    if (!entry.accurate) {
                        MutableComponent text = Component.literal("*").withStyle(ChatFormatting.WHITE);

                        text.setStyle(text.getStyle().withHoverEvent(new HoverEvent.ShowText(Component.literal("This name history entry is not accurate according to laby.net"))));

                        nameText.append(text);
                    }

                    ChatUtils.sendMsg(nameText);
                }
            });

            return SINGLE_SUCCESS;
        }));
    }

    private static class NameHistory {
        public Name[] username_history;
    }

    private static class Name {
        public String name;
        public Date changed_at;
        public boolean accurate;
    }
}
