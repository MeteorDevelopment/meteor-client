/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.Address;
import net.minecraft.client.network.AllowedAddressResolver;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class ServerCommand extends Command {
    private static final List<String> ANTICHEAT_LIST = Arrays.asList("nocheatplus", "negativity", "warden", "horizon", "illegalstack", "coreprotect", "exploitsx", "vulcan", "abc", "spartan", "kauri", "anticheatreloaded", "witherac", "godseye", "matrix", "wraith");
    private int ticks = 0;
    private boolean bukkitVer = false;
    private int completionID = -1;

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

        builder.then(literal("plugins")
            .then(literal("BukkitVer").executes(ctx -> {
                startPluginSearch(true);
                return SINGLE_SUCCESS;
            }))
            .then(literal("MassScan").executes(ctx -> {
                startPluginSearch(false);
                return SINGLE_SUCCESS;
            }))
        );

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

    private void startPluginSearch(boolean bukkit) {
        bukkitVer = bukkit;
        ticks = 0;
        MeteorClient.EVENT_BUS.subscribe(this);
        completionID = new Random().nextInt(200);
        mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(completionID, bukkit ? "bukkit:ver " : "/"));
    }

    private void printPlugins(List<String> plugins) {
        Collections.sort(plugins);

        plugins.replaceAll(this::formatName);

        if (!plugins.isEmpty()) {
            info("Plugins (%d): %s ", plugins.size(), String.join(", ", plugins.toArray(new String[0])));
        } else {
            error("No plugins found.");
        }
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

        MutableText addrText = Text.literal(Formatting.GRAY + server.address);

        addrText.setStyle(addrText.getStyle()
            .withClickEvent(new ClickEvent(
                ClickEvent.Action.COPY_TO_CLIPBOARD,
                server.address
            ))
            .withHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                Text.literal("Copy to clipboard")
            ))
        );
        AllowedAddressResolver.DEFAULT.resolve(ServerAddress.parse(server.address)).map(Address::getInetSocketAddress).ifPresent(inetSocketAddress -> {
            String ip = inetSocketAddress.toString();
            MutableText ipText = Text.literal(String.format("%s (%s)", Formatting.GRAY, ip));
            ipText.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD,
                    ip
                ))
                .withHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text.literal("Copy to clipboard")
                ))
            );
            addrText.append(ipText);
        });
        info(Text.literal(Formatting.GRAY + "%sIP: ").append(addrText));

        info("Port: %d", ServerAddress.parse(server.address).getPort());

        info("Type: %s", mc.player.getServerBrand() != null ? mc.player.getServerBrand() : "unknown");

        info("Motd: %s", server.label != null ? server.label.getString() : "unknown");

        info("Version: %s", server.version.getString());

        info("Protocol version: %d", server.protocolVersion);

        info("Difficulty: %s (Local: %.2f)", mc.world.getDifficulty().getTranslatableName().getString(), mc.world.getLocalDifficulty(mc.player.getBlockPos()).getLocalDifficulty());

        info("Day: %d", mc.world.getTimeOfDay() / 24000L);

        info("Permission level: %s", formatPerms());
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ticks++;

        if (ticks >= 200) {
            error("Plugins check timed out. Either the packet has been dropped, or server has limited you from completion.");
            endPluginSearch();
        }
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        try {
            if (event.packet instanceof CommandSuggestionsS2CPacket packet) {
                if (packet.getCompletionId() != completionID) return;

                List<String> plugins = new ArrayList<>();
                for (Suggestion suggestion : packet.getSuggestions().getList()) {
                    if (bukkitVer) {
                        String pluginName = suggestion.getText();
                        plugins.add(pluginName);
                    } else {
                        String[] command = suggestion.getText().split(":");
                        if (command.length > 1) {
                            String pluginName = command[0].replace("/", "");

                            if (!plugins.contains(pluginName)) {
                                plugins.add(pluginName);
                            }
                        }
                    }
                }

                printPlugins(plugins);
            }
        } catch (Exception e) {
            error("An error occurred while trying to find plugins.");
        } finally {
            endPluginSearch();
        }
    }

    private void endPluginSearch() {
        ticks = 0;
        MeteorClient.EVENT_BUS.unsubscribe(this);
        completionID = -1;
    }

    private String formatName(String name) {
        if (ANTICHEAT_LIST.contains(name.toLowerCase())) {
            return String.format("%s%s(default)", Formatting.RED, name);
        } else if (StringUtils.containsAnyIgnoreCase(name, "exploit", "cheat", "xray", "bot", "illegal")) {
            return String.format("%s%s(default)", Formatting.RED, name);
        }

        return String.format("(highlight)%s(default)", name);
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
}
