package minegame159.meteorclient.gui.screens.topbar;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.TopBarType;
import minegame159.meteorclient.gui.widgets.WWindow;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Color;

public class TopBarConfig extends TopBarScreen {
    public TopBarConfig() {
        super(TopBarType.Config);

        Settings s = new Settings();

        SettingGroup sgGeneral = s.getDefaultGroup();
        SettingGroup sgCategoryColors = s.createGroup("Category Colors");

        sgGeneral.add(new StringSetting.Builder()
                .name("prefix")
                .description("Prefix.")
                .defaultValue(".")
                .onChanged(Config.INSTANCE::setPrefix)
                .onModuleActivated(stringSetting -> stringSetting.set(Config.INSTANCE.getPrefix()))
                .build()
        );

        sgGeneral.add(new BoolSetting.Builder()
                .name("chat-commands-info")
                .description("Send chat message when you use chat comamnds (eg toggling module, changing a setting, etc).")
                .defaultValue(true)
                .onChanged(aBoolean -> Config.INSTANCE.chatCommandsInfo = aBoolean)
                .onModuleActivated(booleanSetting -> booleanSetting.set(Config.INSTANCE.chatCommandsInfo))
                .build()
        );

        for (Category category : ModuleManager.CATEGORIES) {
            sgCategoryColors.add(new ColorSetting.Builder()
                    .name(category.toString().toLowerCase() + "-color")
                    .description(category.toString() + " color.")
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

        WWindow window = add(new WWindow(title, true)).centerXY().getWidget();
        window.add(s.createTable()).fillX().expandX();
    }

    @Override
    public void onClose() {
        Config.INSTANCE.save();
        super.onClose();
    }
}
