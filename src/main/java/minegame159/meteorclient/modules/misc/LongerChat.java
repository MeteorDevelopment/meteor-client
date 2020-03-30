package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;

public class LongerChat extends ToggleModule {
    public Setting<Integer> lines = addSetting(new IntSetting.Builder()
            .name("lines")
            .description("Chat lines.")
            .defaultValue(1000)
            .min(1)
            .build()
    );

    public LongerChat() {
        super(Category.Misc, "longer-chat", "Makes chat longer.");
    }

    public int getMaxLineCount() {
        return isActive() ? lines.get() : 100;
    }
}
