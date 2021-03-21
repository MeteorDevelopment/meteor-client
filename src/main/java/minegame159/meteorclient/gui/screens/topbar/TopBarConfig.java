/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.topbar;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.network.OnlinePlayers;

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

        sgGeneral.add(new BoolSetting.Builder()
                .name("send-data-to-api")
                .description("If checked, your UUID will be send to Meteor's servers.")
                .defaultValue(true)
                .onChanged(aBoolean -> {
                    Config.get().sendDataToApi = aBoolean;
                    OnlinePlayers.forcePing();
                })
                .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().sendDataToApi))
                .build()
        );

        sgGeneral.add(new IntSetting.Builder()
                .name("rotation-hold-ticks")
                .description("Hold long to hold server side rotation when not sending any packets.")
                .defaultValue(9)
                .onChanged(integer -> Config.get().rotationHoldTicks = integer)
                .onModuleActivated(integerSetting -> integerSetting.set(Config.get().rotationHoldTicks))
                .build()
        );

        sgGeneral.add(new BoolSetting.Builder()
                .name("title-screen-credits")
                .description("Show Meteor credits on title screen")
                .defaultValue(true)
                .onChanged(aBool -> Config.get().titleScreenCredits = aBool)
                .onModuleActivated(boolSetting -> boolSetting.set(Config.get().titleScreenCredits))
                .build()
        );

        sgGeneral.add(new BoolSetting.Builder()
                .name("window-title")
                .description("Show Meteor in the window title.")
                .defaultValue(false)
                .onChanged(aBool -> Config.get().windowTitle = aBool)
                .onModuleActivated(boolSetting -> boolSetting.set(Config.get().windowTitle))
                .build()
        );

        add(s.createTable()).fillX().expandX();
    }
}
