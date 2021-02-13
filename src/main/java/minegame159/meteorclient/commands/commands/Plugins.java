package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;

import net.minecraft.command.CommandSource;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.utils.player.ChatUtils;

import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;

import joptsimple.internal.Strings;

import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Plugins extends Command {

    private Integer ticks = 0;

    public Plugins() {
        super("plugins", "Tries to get the server plugins.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ticks = 0;
            MeteorClient.EVENT_BUS.subscribe(this);
            mc.player.networkHandler.sendPacket(new RequestCommandCompletionsC2SPacket(0, "/"));
            return SINGLE_SUCCESS;
        });

    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        ticks++;
        if (ticks >= 5000) {
            ChatUtils.error("Plugins check timed out");
            MeteorClient.EVENT_BUS.unsubscribe(this);
            ticks = 0;
        }
    }

    @EventHandler
    public void onReadPacket(PacketEvent.Receive event) {
        try {
            if (event.packet instanceof CommandSuggestionsS2CPacket) {
                CommandSuggestionsS2CPacket packet = (CommandSuggestionsS2CPacket) event.packet;
                List<String> plugins = new ArrayList<String>();
                Suggestions matches = packet.getSuggestions();
                if (matches == null) {
                    ChatUtils.error("Invalid Packet.");
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

                if (!plugins.isEmpty()) {
                    ChatUtils.info("Plugins (%d): %s ", plugins.size(), Strings.join((String[]) plugins.toArray(new String[0]), ", "));
                } else {
                    ChatUtils.error("No plugins found.");
                }
                ticks = 0;
                MeteorClient.EVENT_BUS.unsubscribe(this);
            }

        } catch (Exception e) {
            ChatUtils.error("En arror occurred while trying to find plugins");
            ticks = 0;
            MeteorClient.EVENT_BUS.unsubscribe(this);
        }
    }
}
