/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import joptsimple.internal.Strings;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.StringHelper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;


import java.util.*;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Eoka extends Command {
    private static final List<String> ANTICHEAT_LIST = Arrays.asList("nocheatplus", "negativity", "warden", "horizon", "illegalstack", "coreprotect", "exploitsx", "vulcan", "abc", "spartan", "kauri", "anticheatreloaded", "witherac", "godseye", "matrix", "wraith");
    private static final String completionStarts = "/:abcdefghijklmnopqrstuvwxyz0123456789-";
    private int ticks = 0;
    private final List<String> plugins = new ArrayList<>();
    private final List<String> players = new ArrayList<>();


    public Eoka() {
        super("Eoka", "Gets Server Plugins to send to eoka db");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

        builder.then(literal("plugins").executes(ctx -> {
                getPlugins();
                return SINGLE_SUCCESS;
            })
        );
        builder.then(literal("players").executes(ctx -> {
            try {
                getPlayers();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return SINGLE_SUCCESS;
            })
        );
    }

    private void getPlayers() throws IOException {
        players.clear();
        String playerName = mc.getSession().getProfile().getName();

        for(PlayerListEntry info : mc.player.networkHandler.getPlayerList()) {
            String name = info.getProfile().getName();
            name = StringHelper.stripTextFormat(name);

            if(name.equalsIgnoreCase(playerName))
                continue;

            players.add(name);
        }
        printPlayers();
    }

    private void printPlayers() throws IOException {
        Collections.sort(players);

        if (!players.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            Object ip = client.getNetworkHandler().getConnection().getAddress().toString();
            String playerList = Strings.join(players.toArray(new String[0]), ", ");
            info("Players (%d): %s ", players.size(), playerList);
            try {
                URL url = new URL("http://localhost:5000/api/players/receive");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                OutputStream os = conn.getOutputStream();
                String jsonInputString = "{ \"ip\": \"" + ip + "\", \"players\": \"" + playerList + "\" }";
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    System.out.println(responseLine);
                }
                os.close();
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            error("No Players found.");
        }

        ticks = 0;
        players.clear();
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }


    private void getPlugins() {
        ticks = 0;
        plugins.clear();
        Random random = new Random();
        MeteorClient.EVENT_BUS.subscribe(this);
        info("Please wait around 5 seconds...");
        (new Thread(() -> completionStarts.chars().forEach(i -> {
            mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(random.nextInt(200), Character.toString(i)));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }))).start();
    }

    private void printPlugins() {
        Collections.sort(plugins);

        if (!plugins.isEmpty()) {
            info("Plugins (%d): %s ", plugins.size(), Strings.join(plugins.toArray(new String[0]), ", "));
        } else {
            error("No plugins found.");
        }

        ticks = 0;
        plugins.clear();
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ticks++;
            if (ticks >= 100) {
                printPlugins();
            }
        }

    @EventHandler
    private void onReadPacket(PacketEvent.Receive event) {
        try {
            if (event.packet instanceof CommandSuggestionsS2CPacket packet) {

                Suggestions matches = packet.getSuggestions();

                if (matches == null) {
                    error("Invalid Packet.");
                    return;
                }

                for (Suggestion suggestion : matches.getList()) {

                        String[] command = suggestion.getText().split(":");
                        if (command.length > 1) {
                            String pluginName = command[0].replace("/", "");

                            if (!plugins.contains(pluginName)) {
                                plugins.add(pluginName);
                            }
                        }
                    }
                }
        } catch (Exception e) {
            error("An error occurred while trying to find plugins.");
        }
    }
}
