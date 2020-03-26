package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.StringSetting;

public class Spam extends Module {
    public static Spam INSTANCE;

    private Setting<String> message = addSetting(new StringSetting.Builder()
            .name("message")
            .description("Message to spam.")
            .defaultValue("Meteor Client")
            .build()
    );

    private Setting<Integer> delay = addSetting(new IntSetting.Builder()
            .name("delay")
            .description("How much ticks to wait between messages. 20 ticks = 1 second.")
            .defaultValue(0)
            .min(0)
            .build()
    );

    private int timer;

    public Spam() {
        super(Category.Misc, "spam", "Spams message in chat.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private Listener<TickEvent> onTick = new Listener<>(event -> {
        timer++;

        if (timer > delay.get()) {
            mc.player.sendChatMessage(message.get());
            timer = 0;
        }
    });
}
