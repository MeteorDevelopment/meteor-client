package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class LongerChat extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    public Setting<Integer> lines = sgGeneral.add(new IntSetting.Builder()
            .name("lines")
            .description("Chat lines.")
            .defaultValue(1000)
            .min(1)
            .sliderMax(1000)
            .build()
    );

    public LongerChat() {
        super(Category.Misc, "longer-chat", "Makes chat longer.");
    }

    public int getMaxLineCount() {
        return isActive() ? lines.get() : 100;
    }
}
