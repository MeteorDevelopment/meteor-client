/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import joptsimple.internal.Strings;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayNetworkHandlerAccessor;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ServerCommand extends Command {
    private static final Set<String> ANTICHEAT_LIST = Set.of("nocheatplus", "negativity", "warden", "horizon", "illegalstack", "coreprotect", "exploitsx", "vulcan", "abc", "spartan", "kauri", "anticheatreloaded", "witherac", "godseye", "matrix", "wraith", "antixrayheuristics", "grimac");
    private static final Set<String> VERSION_ALIASES = Set.of("version", "ver", "about", "bukkit:version", "bukkit:ver", "bukkit:about"); // aliases for bukkit:version
    private String alias;
    private int ticks = 0;
    private boolean tick = false;
    private final List<String> plugins = new ArrayList<>();
    private final List<String> commandTreePlugins = new ArrayList<>();
    private static final Random RANDOM = new Random();


    public ServerCommand() {
        super("server", "Prints server information");

        MeteorClient.EVENT_BUS.subscribe(this);
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
            plugins.addAll(commandTreePlugins);

            if (alias != null) {
                mc.getNetworkHandler().sendPacket(new RequestCommandCompletionsC2SPacket(RANDOM.nextInt(200), alias + " "));
                tick = true;
            } else printPlugins();

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

        MutableText ipText;

        if (ipv4.isEmpty()) {
            ipText = Text.literal(Formatting.GRAY + server.address);
            ipText.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD,
                    server.address
                ))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text.literal("Copy to clipboard")
                ))
            );
        }
        else {
            ipText = Text.literal(Formatting.GRAY + server.address);
            ipText.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD,
                    server.address
                ))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text.literal("Copy to clipboard")
                ))
            );
            MutableText ipv4Text = Text.literal(String.format("%s (%s)", Formatting.GRAY, ipv4));
            ipv4Text.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD,
                    ipv4
                ))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text.literal("Copy to clipboard")
                ))
            );
            ipText.append(ipv4Text);
        }
        info(
            Text.literal(String.format("%sIP: ", Formatting.GRAY))
            .append(ipText)
        );

        info("Port: %d", ServerAddress.parse(server.address).getPort());

        info("Type: %s", mc.getNetworkHandler().getBrand() != null ? mc.getNetworkHandler().getBrand() : "unknown");

        info("Motd: %s", server.label != null ? server.label.getString() : "unknown");

        info("Version: %s", server.version.getString());

        info("Protocol version: %d", server.protocolVersion);

        info("Difficulty: %s (Local: %.2f)", mc.world.getDifficulty().getTranslatableName().getString(), mc.world.getLocalDifficulty(mc.player.getBlockPos()).getLocalDifficulty());

        info("Day: %d", mc.world.getTimeOfDay() / 24000L);

        info("Permission level: %s", formatPerms());
    }

    public String formatPerms() {
        int p = 5;
        while (!mc.player.hasPermissionLevel(p) && p > 0) p--;

        return switch (p) {
            case 0 -> "0 (No Perms)";
            case 1 -> "1 (No Perms)";
            case 2 -> "2 (Player Command Access)";
            case 3 -> "3 (Server Command Access)";
            case 4 -> "4 (Operator)";
            default -> p + " (Unknown)";
        };
    }


    // plugin scanning

    private void printPlugins() {
        plugins.sort(String.CASE_INSENSITIVE_ORDER);
        plugins.replaceAll(this::formatName);

        if (!plugins.isEmpty()) {
            info("Plugins (%d): %s ", plugins.size(), Strings.join(plugins.toArray(new String[0]), ", "));
        } else {
            error("No plugins found.");
        }

        tick = false;
        ticks = 0;
        plugins.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!tick) return;
        ticks++;

        if (ticks >= 100) printPlugins();
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (tick && event.packet instanceof RequestCommandCompletionsC2SPacket) event.cancel();
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        // should return the same set of plugins that command completing '/' would
        // the rationale is that since we should get this packet whenever we log into the server, we can capture it
        // straight away and not need to send a command completion packet for essentially the same results
        if (event.packet instanceof CommandTreeS2CPacket packet) {
            ClientPlayNetworkHandlerAccessor handler = (ClientPlayNetworkHandlerAccessor) event.connection.getPacketListener();
            commandTreePlugins.clear();
            alias = null;

            // This gets the root node of the command tree. From there, all of its children have to be of type
            // LiteralCommandNode, so we don't need to worry about checking or casting and can just grab the name
            packet.getCommandTree(CommandRegistryAccess.of(handler.getCombinedDynamicRegistries(), handler.getEnabledFeatures())).getChildren().forEach(node -> {
                String[] split = node.getName().split(":");
                if (split.length > 1) {
                    if (!commandTreePlugins.contains(split[0])) commandTreePlugins.add(split[0]);
                }

                // checking if any of the bukkit:version commands are available, which we can also grab plugins from
                if (alias == null && VERSION_ALIASES.contains(node.getName())) {
                    alias = node.getName();
                }
            });

        }

        if (!tick) return;

        try {
            if (event.packet instanceof CommandSuggestionsS2CPacket packet) {
                Suggestions matches = packet.getSuggestions();

                if (matches.isEmpty()) {
                    error("An error occurred while trying to find plugins.");
                    return;
                }

                for (Suggestion suggestion : matches.getList()) {
                    String pluginName = suggestion.getText();
                    if (!plugins.contains(pluginName.toLowerCase())) plugins.add(pluginName);
                }

                printPlugins();
            }
        } catch (Exception e) {
            error("An error occurred while trying to find plugins.");
        }
    }

    private String formatName(String name) {
        if (ANTICHEAT_LIST.contains(name.toLowerCase())) {
            return String.format("%s%s(default)", Formatting.RED, name);
        }
        else if (StringUtils.containsIgnoreCase(name, "exploit") || StringUtils.containsIgnoreCase(name, "cheat") || StringUtils.containsIgnoreCase(name, "illegal")) {
            return String.format("%s%s(default)", Formatting.RED, name);
        }

        return String.format("(highlight)%s(default)", name);
    }
}
