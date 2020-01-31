package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.IntSettingBuilder;

public class LongerChat extends Module {
    public Setting<Integer> lines = addSetting(new IntSettingBuilder()
            .name("lines")
            .description("Chat lines.")
            .defaultValue(1000)
            .min(1)
            .consumer((integer, integer2) -> {
                if (isActive()) MeteorClient.eventBus.post(EventStore.changeChatLengthEvent(integer2));
            })
            .build()
    );

    public LongerChat() {
        super(Category.Misc, "longer-chat", "Makes chat longer.");
    }

    @Override
    public void onActivate() {
        MeteorClient.eventBus.post(EventStore.changeChatLengthEvent(lines.value()));
    }

    @Override
    public void onDeactivate() {
        MeteorClient.eventBus.post(EventStore.changeChatLengthEvent(100));
    }
}
