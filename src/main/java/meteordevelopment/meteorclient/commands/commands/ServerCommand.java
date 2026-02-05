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
import meteordevelopment.meteorclient.mixin.ClientPlayNetworkHandlerAccessor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.DefaultPermissions;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.Strings;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ServerCommand extends Command {
    private static final Set<String> ANTICHEAT_LIST = Set.of("nocheatplus", "negativity", "warden", "horizon", "illegalstack", "coreprotect", "exploitsx", "vulcan", "abc", "spartan", "kauri", "anticheatreloaded", "witherac", "godseye", "matrix", "wraith", "antixrayheuristics", "grimac", "themis", "foxaddition", "guardianac", "ggintegrity", "lightanticheat", "anarchyexploitfixes");
    private static final Set<String> VERSION_ALIASES = Set.of("version", "ver", "about", "bukkit:version", "bukkit:ver", "bukkit:about"); // aliases for bukkit:version
    private String alias;
    private int ticks = 0;
    private boolean tick = false;
    private final List<String> plugins = new ArrayList<>();
    private final List<String> commandTreePlugins = new ArrayList<>();
    private static final Random RANDOM = new Random();


    public ServerCommand() {
        super("server");

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
            info("tps", Text.literal(String.format("%.2f", tps)).formatted(color));
            return SINGLE_SUCCESS;
        }));
    }

    private void basicInfo() {
        if (mc.isIntegratedServerRunning()) {
            IntegratedServer server = mc.getServer();

            info("singleplayer");
            if (server != null) info("version", server.getVersion());

            return;
        }

        ServerInfo server = mc.getCurrentServerEntry();

        if (server == null) {
            error("cant_obtain_info");
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
                .withClickEvent(new ClickEvent.CopyToClipboard(server.address))
                .withHoverEvent(new HoverEvent.ShowText(MeteorClient.translatable("command.server.info.copy")))
            );
        }
        else {
            ipText = Text.literal(Formatting.GRAY + server.address);
            ipText.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent.CopyToClipboard(server.address))
                .withHoverEvent(new HoverEvent.ShowText(MeteorClient.translatable("command.server.info.copy")))
            );
            MutableText ipv4Text = Text.literal(String.format("%s (%s)", Formatting.GRAY, ipv4));
            ipv4Text.setStyle(ipText.getStyle()
                .withClickEvent(new ClickEvent.CopyToClipboard(ipv4))
                .withHoverEvent(new HoverEvent.ShowText(MeteorClient.translatable("command.server.info.copy")))
            );
            ipText.append(ipv4Text);
        }
        info("ip", ipText);

        info("port", ServerAddress.parse(server.address).getPort());
        info("type", mc.getNetworkHandler().getBrand() != null ? mc.getNetworkHandler().getBrand() : MeteorClient.translatable("command.server.info.unknown"));
        info("motd", server.label != null ? server.label.getString() : MeteorClient.translatable("command.server.info.unknown"));
        info("version", server.version.getString());
        info("protocol_version", server.protocolVersion);
        info("difficulty",
            mc.world.getDifficulty().getTranslatableName(),
            new LocalDifficulty(
                mc.world.getDifficulty(),
                mc.world.getTimeOfDay(),
                mc.world.getChunk(mc.player.getBlockPos()).getInhabitedTime(),
                DimensionType.MOON_SIZES[mc.world.getEnvironmentAttributes().getAttributeValue(EnvironmentAttributes.MOON_PHASE_VISUAL, mc.player.getBlockPos()).getIndex()] // lol
            ).getLocalDifficulty()
        );
        info("day", mc.world.getTimeOfDay() / 24000L);
        info(formatPerms());
    }

    public String formatPerms() {
        PermissionPredicate permissions = mc.player.getPermissions();

        if (permissions.hasPermission(DefaultPermissions.OWNERS)) return "permission_owner";
        else if (permissions.hasPermission(DefaultPermissions.ADMINS)) return "permission_admin";
        else if (permissions.hasPermission(DefaultPermissions.GAMEMASTERS)) return "permission_gamemaster";
        else if (permissions.hasPermission(DefaultPermissions.MODERATORS)) return "permission_moderator";
        else return "permission_player";
    }


    // plugin scanning

    private void printPlugins() {
        plugins.sort(String.CASE_INSENSITIVE_ORDER);
        List<Text> pluginTexts = new ArrayList<>();
        for (String plugin : plugins) {
            pluginTexts.add(formatName(plugin));
        }

        if (!plugins.isEmpty()) {
            info("plugins", plugins.size(), Texts.join(pluginTexts, Texts.DEFAULT_SEPARATOR_TEXT));
        } else {
            error("no_plugins");
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
            packet.getCommandTree(
                CommandRegistryAccess.of(handler.meteor$getCombinedDynamicRegistries(), handler.meteor$getEnabledFeatures()),
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
            if (event.packet instanceof CommandSuggestionsS2CPacket packet) {
                Suggestions matches = packet.getSuggestions();

                if (matches.isEmpty()) {
                    error("plugins");
                    return;
                }

                for (Suggestion suggestion : matches.getList()) {
                    String pluginName = suggestion.getText();
                    if (!plugins.contains(pluginName.toLowerCase())) plugins.add(pluginName);
                }

                printPlugins();
            }
        } catch (Exception e) {
            error("plugins");
        }
    }

    private Text formatName(String name) {
        if (ANTICHEAT_LIST.contains(name.toLowerCase())) {
            return Text.literal(name).formatted(Formatting.RED);
        }
        else if (Strings.CI.contains(name, "exploit") || Strings.CI.contains(name, "cheat") || Strings.CI.contains(name, "illegal")) {
            return Text.literal(name).formatted(Formatting.RED);
        }

        return ChatUtils.highlight(name);
    }
}
