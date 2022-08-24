/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.google.common.reflect.TypeToken;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.systems.commands.arguments.PlayerListEntryArgumentType;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class NameHistoryCommand extends Command {
    private static final Type RESPONSE_TYPE = new TypeToken<List<NameHistoryObject>>() {}.getType();

    public NameHistoryCommand() {
        super("name-history", "Provides a list of a players previous names from the Mojang api.", "history", "names");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerListEntryArgumentType.create()).executes(context -> {
            MeteorExecutor.execute(() -> {
                PlayerListEntry lookUpTarget = PlayerListEntryArgumentType.get(context);
                UUID uuid = lookUpTarget.getProfile().getId();

                List<NameHistoryObject> nameHistoryObjects = Http.get("https://api.mojang.com/user/profiles/" + formatUUID(uuid) + "/names").sendJson(RESPONSE_TYPE);

                if (nameHistoryObjects == null || nameHistoryObjects.isEmpty()) {
                    error("There was an error fetching that users name history.");
                    return;
                }

                String name = lookUpTarget.getProfile().getName();
                MutableText initial = Text.literal(name);
                initial.append(Text.literal(name.endsWith("s") ? "'" : "'s"));

                Color nameColor = PlayerUtils.getPlayerColor(mc.world.getPlayerByUuid(uuid), Utils.WHITE);

                initial.setStyle(initial.getStyle()
                        .withColor(new TextColor(nameColor.getPacked()))
                        .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.OPEN_URL,
                                        "https://namemc.com/search?q=" + name
                                )
                        )
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Text.literal("View on NameMC")
                                        .formatted(Formatting.YELLOW)
                                        .formatted(Formatting.ITALIC)
                        ))
                );

                info(initial.append(Text.literal(" Username History:").formatted(Formatting.GRAY)));

                for (NameHistoryObject nameHistoryObject : nameHistoryObjects) {
                    MutableText nameText = Text.literal(nameHistoryObject.name);
                    nameText.formatted(Formatting.AQUA);

                    if (nameHistoryObject.changedToAt != 0L) {
                        MutableText changed = Text.literal("Changed at: ");
                        changed.formatted(Formatting.GRAY);

                        Date date = new Date(nameHistoryObject.changedToAt);
                        DateFormat formatter = new SimpleDateFormat("hh:mm:ss, dd/MM/yyyy");
                        changed.append(Text.literal(formatter.format(date)).formatted(Formatting.WHITE));

                        nameText.setStyle(nameText.getStyle().withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, changed)));
                    }

                    ChatUtils.sendMsg(nameText);
                }
            });

            return SINGLE_SUCCESS;
        }));
    }

    private String formatUUID(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    private static class NameHistoryObject {
        String name;
        long changedToAt;
    }
}
