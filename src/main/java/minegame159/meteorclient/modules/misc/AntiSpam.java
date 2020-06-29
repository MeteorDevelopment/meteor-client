package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class AntiSpam extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> depth = sgGeneral.add(new IntSetting.Builder()
            .name("depth")
            .description("How many messages to check for duplicates.")
            .defaultValue(4)
            .min(1)
            .sliderMin(1)
            .build()
    );

    private final Setting<Boolean> moveToBottom = sgGeneral.add(new BoolSetting.Builder()
            .name("move-to-bottom")
            .description("Move duplicate messages to bottom.")
            .defaultValue(true)
            .build()
    );

    public AntiSpam() {
        super(Category.Misc, "anti-spam", "Repeated messages not shown.");
    }

    public int getDepth() {
        return isActive() ? depth.get() : 0;
    }

    public boolean isMoveToBottom() {
        return moveToBottom.get();
    }
}
