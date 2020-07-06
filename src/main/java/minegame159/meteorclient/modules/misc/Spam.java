package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.StringSetting;

public class Spam extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
            .name("message")
            .description("Message to spam.")
            .defaultValue("Meteor on Crack!")
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("How much ticks to wait between messages. 20 ticks = 1 second.")
            .defaultValue(60)
            .min(0)
            .sliderMax(60)
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
