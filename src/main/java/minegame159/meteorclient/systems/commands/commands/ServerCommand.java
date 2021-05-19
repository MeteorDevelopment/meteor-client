/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import joptsimple.internal.Strings;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.systems.commands.Command;
import minegame159.meteorclient.utils.world.TickRate;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.network.ServerAddress;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.BaseText;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ServerCommand extends Command {
    private static final List<String> ANTICHEAT_LIST = Arrays.asList("nocheatplus", "negativity", "warden", "horizon","illegalstack","coreprotect","exploitsx");
    private Integer ticks = 0;

    public ServerCommand() {
        super("server", "Prints server information");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            basicInfo();
            return SINGLE_SUCCESS;
        });

        builder.then(literal("info").executes(ctx -> {
            basicInfo();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("plugins").executes(ctx -> {
            ticks = 0;
            MeteorClient.EVENT_BUS.subscribe(this);
            mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(0, "/"));
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("tps").executes(ctx -> {
            float tps = TickRate.INSTANCE.getTickRate();
            Formatting color;
            if (tps > 17.0f) color = Formatting.GREEN;
            else if (tps > 12.0f) color = Formatting.YELLOW;
            else color = Formatting.RED;
            info("Current TPS: %s%.2f(default).", color, tps);
            return SINGLE_SUCCESS;
        }));
    }

    private void basicInfo() {
        if (mc.isIntegratedServerRunning()) {
            IntegratedServer server = mc.getServer();

            info("Singleplayer");
            if (server != null) info("Version: %s", server.getVersion());

            return;
        }

        ServerInfo server = mc.getCurrentServerEntry();

        if (server == null) {
            info("Couldn't obtain any server information.");
            return;
        }

        String ipv4 = "";
        try {
            ipv4 = InetAddress.getByName(server.address).getHostAddress();
        } catch (UnknownHostException ignored) {}

        BaseText ipText;

        if (ipv4.isEmpty()) {
            ipText = new LiteralText(Formatting.GRAY + server.address);
            ipText.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD, 
                    server.address
                ))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT, 
                    new LiteralText("Copy to clipboard")
                ))
            );
        }
        else {
            ipText = new LiteralText(Formatting.GRAY + server.address);
            ipText.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD, 
                    server.address
                ))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT, 
                    new LiteralText("Copy to clipboard")
                ))
            );
            BaseText ipv4Text = new LiteralText(String.format("%s (%s)", Formatting.GRAY, ipv4));
            ipv4Text.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD, 
                    ipv4
                ))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT, 
                    new LiteralText("Copy to clipboard")
                ))
            );
            ipText.append(ipv4Text);
        }
        info(
            new LiteralText(String.format("%sIP: ", Formatting.GRAY))
            .append(ipText)
        );

        info("Port: %d", ServerAddress.parse(server.address).getPort());

        info("Type: %s", mc.player.getServerBrand() != null ? mc.player.getServerBrand() : "unknown");

        info("Motd: %s", server.label != null ? server.label.getString() : "unknown");

        info("Version: %s", server.version.getString());

        info("Protocol version: %d", server.protocolVersion);

        info("Difficulty: %s", mc.world.getDifficulty().getTranslatableName().getString());

        ClientCommandSource cmdSource = mc.getNetworkHandler().getCommandSource();
        int permission_level = 5;
        while (permission_level > 0) {
            if (cmdSource.hasPermissionLevel(permission_level)) break;
            permission_level--;
        }
        info("Permission level: %d", permission_level);
    }
    
    @EventHandler
    public void onTick(TickEvent.Post event) {
        ticks++;

        if (ticks >= 5000) {
            error("Plugins check timed out");
            MeteorClient.EVENT_BUS.unsubscribe(this);
            ticks = 0;
        }
    }

    @EventHandler
    public void onReadPacket(PacketEvent.Receive event) {
        try {
            if (event.packet instanceof CommandSuggestionsS2CPacket) {
                CommandSuggestionsS2CPacket packet = (CommandSuggestionsS2CPacket) event.packet;
                List<String> plugins = new ArrayList<>();
                Suggestions matches = packet.getSuggestions();

                if (matches == null) {
                    error("Invalid Packet.");
                    return;
                }

                for (Suggestion yes : matches.getList()) {
                    String[] command = yes.getText().split(":");
                    if (command.length > 1) {
                        String pluginName = command[0].replace("/", "");

                        if (!plugins.contains(pluginName)) {
                            plugins.add(pluginName);
                        }
                    }
                }

                Collections.sort(plugins);
                for (int i = 0; i < plugins.size(); i++) {
                    plugins.set(i, formatName(plugins.get(i)));
                }

                if (!plugins.isEmpty()) {
                    info("Plugins (%d): %s ", plugins.size(), Strings.join(plugins.toArray(new String[0]), ", "));
                } else {
                    error("No plugins found.");
                }

                ticks = 0;
                MeteorClient.EVENT_BUS.unsubscribe(this);
            }

        } catch (Exception e) {
            error("An error occurred while trying to find plugins");
            ticks = 0;
            MeteorClient.EVENT_BUS.unsubscribe(this);
        }
    }

    private String formatName(String name) {
        if (ANTICHEAT_LIST.contains(name)) {
            return String.format("%s%s(default)", Formatting.RED, name);
        }
        else if (name.contains("exploit") || name.contains("cheat") || name.contains("illegal")) {
            return String.format("%s%s(default)", Formatting.RED, name);
        }

        return String.format("(highlight)%s(default)", name);
    }
}
