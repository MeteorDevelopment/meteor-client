package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.IntSettingBuilder;

public class LongerChat extends Module {
    public static boolean active;
    public static int linesInt;

    public Setting<Integer> lines = addSetting(new IntSettingBuilder()
            .name("lines")
            .description("Chat lines.")
            .defaultValue(1000)
            .min(1)
            .consumer((oldValue, newValue) -> linesInt = newValue)
            .build()
    );

    public LongerChat() {
        super(Category.Misc, "longer-chat", "Makes chat longer.");
    }

    @Override
    public void onActivate() {
        active = true;
    }

    @Override
    public void onDeactivate() {
        active = false;
    }
}
