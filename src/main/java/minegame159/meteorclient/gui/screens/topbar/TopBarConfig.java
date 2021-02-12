/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.topbar;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.Settings;
import minegame159.meteorclient.settings.StringSetting;

public class TopBarConfig extends TopBarWindowScreen {
    public TopBarConfig() {
        super(TopBarType.Config);
    }

    @Override
    protected void initWidgets() {
        Settings s = new Settings();

        SettingGroup sgGeneral = s.getDefaultGroup();

        sgGeneral.add(new StringSetting.Builder()
                .name("prefix")
                .description("Prefix.")
                .defaultValue(".")
                .onChanged(Config.get()::setPrefix)
                .onModuleActivated(stringSetting -> stringSetting.set(Config.get().getPrefix()))
                .build()
        );

        sgGeneral.add(new BoolSetting.Builder()
                .name("custom-font")
                .description("Use a custom font.")
                .defaultValue(true)
                .onChanged(aBoolean -> {
                    Config.get().customFont = aBoolean;
                    root.invalidate();
                })
                .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().customFont))
                .build()
        );

        sgGeneral.add(new BoolSetting.Builder()
                .name("chat-commands-info")
                .description("Sends a chat message when you use chat comamnds (eg toggling module, changing a setting, etc).")
                .defaultValue(true)
                .onChanged(aBoolean -> Config.get().chatCommandsInfo = aBoolean)
                .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().chatCommandsInfo))
                .build()
        );

        sgGeneral.add(new BoolSetting.Builder()
                .name("delete-chat-commands-info")
                .description("Delete previous chat messages.")
                .defaultValue(true)
                .onChanged(aBoolean -> Config.get().deleteChatCommandsInfo = aBoolean)
                .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().deleteChatCommandsInfo))
                .build()
        );

        add(s.createTable()).fillX().expandX();
    }
}
