package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class MapPreview extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public MapPreview() {
        super(Categories.Misc, "map-preview", "View your map art from your inventory.");
    }

    private final Setting<Integer> scale = sgGeneral.add(new IntSetting.Builder()
        .name("scale")
        .description("The scale of the map.")
        .defaultValue(100)
        .min(1)
        .sliderMax(500)
        .build()
    );

    public int getScale() {
        return scale.get();
    }
}
