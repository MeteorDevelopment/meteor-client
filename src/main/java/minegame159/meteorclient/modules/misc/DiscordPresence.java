package minegame159.meteorclient.modules.misc;

//Created by squidoodly 12/05/2020

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import org.json.JSONObject;

import java.time.OffsetDateTime;

public class DiscordPresence extends ToggleModule {
    private enum SmallImage {
        MineGame("minegame", "MineGame159"),
        Squid("squidoodly", "squidoodly");

        private final String key, text;

        SmallImage(String key, String text) {
            this.key = key;
            this.text = text;
        }

        void apply(RichPresence.Builder presence) {
            presence.setSmallImage(key, text);
        }

        SmallImage next() {
            if (this == MineGame) return Squid;
            return MineGame;
        }
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> displayName = sgGeneral.add(new BoolSetting.Builder()
            .name("display-name")
            .description("Displays your name in discord rpc.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayServer = sgGeneral.add(new BoolSetting.Builder()
            .name("display-server")
            .description("Displays the server you are in.")
            .defaultValue(true)
            .build()
    );

    private IPCClient client;
    private final RichPresence.Builder presence = new RichPresence.Builder();

    private boolean ready;
    private int ticks;
    private SmallImage currentSmallImage;

    public DiscordPresence() {
        super(Category.Misc, "discord-presence", "That stuff you see in discord");

        client = new IPCClient(709793491911180378L);
        client.setListener(new IPCListener() {
            @Override
            public void onReady(IPCClient client) {
                ready = true;

                presence.setStartTimestamp(OffsetDateTime.now());
                presence.setDetails(getText());
                presence.setLargeImage("meteor_client", "https://meteorclient.com/");
                currentSmallImage.apply(presence);

                client.sendRichPresence(presence.build());
            }

            @Override
            public void onClose(IPCClient client, JSONObject json) {
                ready = false;
            }

            @Override
            public void onDisconnect(IPCClient client, Throwable t) {
                ready = false;
            }
        });
    }

    @Override
    public void onActivate(){
        ticks = 0;
        currentSmallImage = SmallImage.MineGame;

        try {
            client.connect();
        } catch (NoDiscordClientException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDeactivate(){
        client.close();
    }

    @EventHandler
    private final Listener<TickEvent> OnTick = new Listener<>(event -> {
        if (ready) {
            ticks++;

            if (ticks >= 200) {
                currentSmallImage = currentSmallImage.next();
                currentSmallImage.apply(presence);
                client.sendRichPresence(presence.build());

                ticks = 0;
            }
        }
    });

    private String getText() {
        if (mc.isInSingleplayer()) {
            if (displayName.get()) return getName() + " || SinglePlayer";
            else return "SinglePlayer";
        }

        if (displayName.get() && displayServer.get()) return getName() + " || " + getServer();
        else if (!displayName.get() && displayServer.get()) return getServer();
        else if (displayName.get() && !displayServer.get()) return getName();

        return "";
    }

    private String getServer(){
        return Utils.getWorldName();
    }

    private String getName(){
        return mc.player.getEntityName();
    }
}
