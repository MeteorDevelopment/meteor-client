package minegame159.meteorclient.modules.movement;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;

public class HighJump extends Module {
    private Setting<Double> multiplier = addSetting(new DoubleSetting.Builder()
            .name("multiplier")
            .description("Jump height multiplier.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    public HighJump() {
        super(Category.Movement, "high-jump", "Jump higher.");
    }

    public float getMultiplier() {
        return multiplier.get().floatValue();
    }
}
