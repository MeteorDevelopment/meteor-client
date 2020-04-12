package minegame159.meteorclient.modules.movement;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;

public class AntiLevitation extends ToggleModule {
    private Setting<Boolean> applyGravity = addSetting(new BoolSetting.Builder()
            .name("apply-gravity")
            .description("Apply gravity.")
            .defaultValue(false)
            .build()
    );

    public AntiLevitation() {
        super(Category.Movement, "anti-levitation", "Removes levitation effect.");
    }

    public boolean isApplyGravity() {
        return applyGravity.get();
    }
}
