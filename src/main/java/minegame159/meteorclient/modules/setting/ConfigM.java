package minegame159.meteorclient.modules.setting;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.StringSetting;
import minegame159.meteorclient.utils.Color;

public class ConfigM extends Module {
    public ConfigM() {
        super(Category.Setting, "config", "Config.");
        serialize = false;

        addSetting(new StringSetting.Builder()
                .name("prefix")
                .description("Prefix.")
                .group("General")
                .defaultValue(".")
                .onChanged(Config.INSTANCE::setPrefix)
                .onModuleActivated(stringSetting -> stringSetting.set(Config.INSTANCE.getPrefix()))
                .build()
        );

        for (Category category : ModuleManager.CATEGORIES) {
            addSetting(new ColorSetting.Builder()
                    .name(category.toString().toLowerCase() + "-color")
                    .description(category.toString() + " color.")
                    .group("Category Colors")
                    .defaultValue(new Color(0, 0, 0, 0))
                    .onChanged(color1 -> Config.INSTANCE.setCategoryColor(category, color1))
                    .onModuleActivated(colorSetting -> {
                        Color color = Config.INSTANCE.getCategoryColor(category);
                        if (color == null) color = new Color(0, 0, 0, 0);
                        colorSetting.set(color);
                    })
                    .build()
            );
        }
    }
}
