package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;

public class LongerChat extends Module {
    public Setting<Integer> lines = addSetting(new IntSetting.Builder()
            .name("lines")
            .description("Chat lines.")
            .defaultValue(1000)
            .min(1)
            .onChanged(integer -> {
                if (isActive()) MeteorClient.eventBus.post(EventStore.changeChatLengthEvent(integer));
            })
            .build()
    );

    public LongerChat() {
        super(Category.Misc, "longer-chat", "Makes chat longer.");
    }

    @Override
    public void onActivate() {
        MeteorClient.eventBus.post(EventStore.changeChatLengthEvent(lines.get()));
    }

    @Override
    public void onDeactivate() {
        MeteorClient.eventBus.post(EventStore.changeChatLengthEvent(100));
    }
}
