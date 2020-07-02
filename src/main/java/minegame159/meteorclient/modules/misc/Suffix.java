package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.SendMessageEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.StringSetting;

public class Suffix extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> text = sgGeneral.add(new StringSetting.Builder()
            .name("text")
            .description("Text to add.")
            .defaultValue(" - Meteor on Crack!")
            .build()
    );

    public Suffix() {
        super(Category.Misc, "suffix", "Adds a suffix after every message you send.");
    }

    @EventHandler
    private final Listener<SendMessageEvent> onSendMessage = new Listener<>(event -> {
        event.msg += text.get();
    });
}
