package minegame159.meteorclient.systems.hud;

import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;

public abstract class ScaleableHudElement extends HudElement {

    private final Setting<Double> scale = sgBox.add(new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of the element.")
            .defaultValue(2)
            .min(0.1)
            .sliderMin(0.1)
            .build()
    );

    public ScaleableHudElement(String name, String description) {
        super(name, description);
    }

    public double getScale() {
        return scale.get() * hud.scale.get();
    }

    public void setScale(double scale) {
        this.scale.set(scale);
    }

    public void addScale(double scale) {
        this.scale.set(this.scale.get() + scale);
    }
}
