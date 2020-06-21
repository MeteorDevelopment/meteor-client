package minegame159.meteorclient.modules.misc;

//Created by squidoodly 12/05/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.utils.Utils;
import net.arikia.dev.drpc.DiscordEventHandlers;
import net.arikia.dev.drpc.DiscordRPC;
import net.arikia.dev.drpc.DiscordRichPresence;

public class DiscordPresence extends ToggleModule {
    private final static DiscordRichPresence presence = new DiscordRichPresence.Builder("https://discord.gg/BG2kMWb").build();

    public DiscordPresence(){super(Category.Misc, "discord-presence", "That stuff you see in discord");}

    private int ticks = 0;

    @EventHandler
    private Listener<TickEvent> OnTick = new Listener<>(event -> {
        ticks++;
        if(ticks < 200){
            DiscordPresence.presence.smallImageKey = "minegame";
            DiscordPresence.presence.smallImageText = "MineGame159";
        }else if(ticks < 400){
            DiscordPresence.presence.smallImageKey = "squidoodly";
            DiscordPresence.presence.smallImageText = "squidoodly";
        }else{
            ticks = 0;
        }
        DiscordRPC.discordUpdatePresence(presence);
        DiscordRPC.discordRunCallbacks();
    });

    @Override
    public void onActivate(){
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
                .setReadyEventHandler(user -> {System.out.println("Initializing Discord Presence");}).build();
        DiscordRPC.discordInitialize("709793491911180378", handlers, true);
        DiscordPresence.presence.startTimestamp = System.currentTimeMillis()/1000;
        if(mc.isInSingleplayer()) {
            DiscordPresence.presence.details = getName() + " || SinglePlayer";
        }else{
            DiscordPresence.presence.details = getName() + " || " + getServer();
        }
        DiscordPresence.presence.largeImageKey = "meteor_client";
        DiscordPresence.presence.largeImageText = "https://meteorclient.github.io/";
        DiscordRPC.discordUpdatePresence(presence);
    }

    @Override
    public void onDeactivate(){
        DiscordRPC.discordShutdown();
    }

    private String getServer(){
        return Utils.getWorldName();
    }

    private String getName(){
        return mc.player.getEntityName();
    }
}
