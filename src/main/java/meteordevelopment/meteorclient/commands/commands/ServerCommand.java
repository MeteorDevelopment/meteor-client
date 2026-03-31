// TODO(Ravel): Failed to fully resolve file: null cannot be cast to non-null type com.intellij.psi.PsiClass
/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientPacketListenerAccessor;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.commons.lang3.Strings;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ServerCommand extends Command {
    private static final Set<String> ANTICHEAT_LIST = Set.of("nocheatplus", "negativity", "warden", "horizon", "illegalstack", "coreprotect", "exploitsx", "vulcan", "abc", "spartan", "kauri", "anticheatreloaded", "witherac", "godseye", "matrix", "wraith", "antixrayheuristics", "grimac", "themis", "foxaddition", "guardianac", "ggintegrity", "lightanticheat", "anarchyexploitfixes", "polar");
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
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
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
            ChatFormatting color;
            if (tps > 17.0f) color = ChatFormatting.GREEN;
            else if (tps > 12.0f) color = ChatFormatting.YELLOW;
            else color = ChatFormatting.RED;
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

        ServerData server = mc.getCurrentServerEntry();

        if (server == null) {
            info("Couldn't obtain any server information.");
            return;
        }

        String ipv4 = "";
        try {
            ipv4 = InetAddress.getByName(server.address).getHostAddress();
        } catch (UnknownHostException ignored) {
        }

        MutableComponent ipText;

        if (ipv4.isEmpty()) {
            ipText = MutableComponent.literal(ChatFormatting.GRAY + server.address);
            ipText.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent.CopyToClipboard(server.address))
                .withHoverEvent(new HoverEvent.ShowText(MutableComponent.literal("Copy to clipboard")))
            );
        } else {
            ipText = MutableComponent.literal(ChatFormatting.GRAY + server.address);
            ipText.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent.CopyToClipboard(server.address))
                .withHoverEvent(new HoverEvent.ShowText(MutableComponent.literal("Copy to clipboard")))
            );
            MutableComponent ipv4Text = MutableComponent.literal(String.format("%s (%s)", ChatFormatting.GRAY, ipv4));
            ipv4Text.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent.CopyToClipboard(ipv4))
                .withHoverEvent(new HoverEvent.ShowText(MutableComponent.literal("Copy to clipboard")))
            );
            ipText.append(ipv4Text);
        }
        info(
            MutableComponent.literal(String.format("%sIP: ", ChatFormatting.GRAY))
                .append(ipText)
        );

        info("Port: %d", ServerAddress.parse(server.address).getPort());
        info("Type: %s", mc.getNetworkHandler().getBrand() != null ? mc.getNetworkHandler().getBrand() : "unknown");
        info("Motd: %s", server.label != null ? server.label.getString() : "unknown");
        info("Version: %s", server.version.getString());
        info("Protocol version: %d", server.protocolVersion);
        info("Difficulty: %s (Local: %.2f)",
            mc.world.getDifficulty().getTranslatableName().getString(),
            new LocalDifficulty(
                mc.world.getDifficulty(),
                mc.world.getTimeOfDay(),
                mc.world.getChunk(mc.player.getBlockPos()).getInhabitedTime(),
                DimensionType.MOON_SIZES[mc.world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.MOON_PHASE_VISUAL, mc.player.getBlockPos()).getIndex()] // lol
            ).getLocalDifficulty()
        );
        info("Day: %d", mc.world.getTimeOfDay() / 24000L);
        info("Permission level: %s", formatPerms());
    }

    public String formatPerms() {
        PermissionSet permissions = mc.player.getPermissions();

        if (permissions.hasPermission(Permissions.OWNERS)) return "4 (Owner)";
        else if (permissions.hasPermission(Permissions.ADMINS)) return "3 (Admin)";
        else if (permissions.hasPermission(Permissions.GAMEMASTERS)) return "2 (Gamemaster)";
        else if (permissions.hasPermission(Permissions.MODERATORS)) return "1 (Moderator)";
        else return "0 (No Perms)";
    }


    // plugin scanning

    private void printPlugins() {
        plugins.sort(String.CASE_INSENSITIVE_ORDER);
        plugins.replaceAll(this::formatName);

        if (!plugins.isEmpty()) {
            info("Plugins (%d): %s ", plugins.size(), String.join(", ", plugins));
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
        if (tick && event.packet instanceof ServerboundCommandSuggestionPacket) event.cancel();
    }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        // should return the same set of plugins that command completing '/' would
        // the rationale is that since we should get this packet whenever we log into the server, we can capture it
        // straight away and not need to send a command completion packet for essentially the same results
        if (event.packet instanceof ClientboundCommandsPacket packet) {
            ClientPacketListenerAccessor handler = (ClientPacketListenerAccessor) event.connection.getPacketListener();
            commandTreePlugins.clear();
            alias = null;

            // This gets the root node of the command tree. From there, all of its children have to be of type
            // LiteralCommandNode, so we don't need to worry about checking or casting and can just grab the name
            packet.getCommandTree(
                CommandBuildContext.of(handler.meteor$getCombinedDynamicRegistries(), handler.meteor$getEnabledFeatures()),
                ClientPlayNetworkHandlerAccessor.meteor$getCommandNodeFactory()
            ).getChildren().forEach(node -> {
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
            if (event.packet instanceof ClientboundCommandSuggestionsPacket packet) {
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
            return String.format("%s%s(default)", ChatFormatting.RED, name);
        } else if (Strings.CI.contains(name, "exploit") || Strings.CI.contains(name, "cheat") || Strings.CI.contains(name, "illegal")) {
            return String.format("%s%s(default)", ChatFormatting.RED, name);
        }

        return String.format("(highlight)%s(default)", name);
    }
}
