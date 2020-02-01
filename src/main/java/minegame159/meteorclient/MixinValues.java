package minegame159.meteorclient;

import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.BetterShulkerTooltipEvent;
import minegame159.meteorclient.events.ChangeChatLengthEvent;

public class MixinValues {
    private static int chatLength = 100;
    private static boolean betterShulkerTooltip = false;

    public static void init() {
        MeteorClient.eventBus.subscribe(new Listener<ChangeChatLengthEvent>(event -> chatLength = event.length));
        MeteorClient.eventBus.subscribe(new Listener<BetterShulkerTooltipEvent>(event -> betterShulkerTooltip = event.enabled));
    }

    public static int getChatLength() {
        return chatLength;
    }

    public static boolean isBetterShulkerTooltip() {
        return betterShulkerTooltip;
    }
}
