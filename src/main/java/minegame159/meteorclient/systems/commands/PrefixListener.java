package minegame159.meteorclient.systems.commands;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.meteor.CharTypedEvent;
import minegame159.meteorclient.systems.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;

public class PrefixListener {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Config config = Config.get();

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(PrefixListener.class);
    }

    @EventHandler
    private static void onCharTyped(CharTypedEvent event) {
        if (mc.currentScreen != null) return;
        if (!config.openChatOnPrefix) return;

        if (event.c == config.prefix.charAt(0)) {
            mc.openScreen(new ChatScreen(config.prefix));
            event.cancel();
        }
    }
}
