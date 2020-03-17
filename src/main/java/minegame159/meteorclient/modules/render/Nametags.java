package minegame159.meteorclient.modules.render;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;

public class Nametags extends Module {
    private Setting<Double> scale = addSetting(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    public Nametags() {
        super(Category.Render, "nametags", "Displays nametags above players.");
    }

    public double getScale() {
        return scale.get();
    }
}
