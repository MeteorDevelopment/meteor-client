/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.tabs.builtin;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.tabs.Tab;
import minegame159.meteorclient.gui.tabs.TabScreen;
import minegame159.meteorclient.gui.tabs.WindowTabScreen;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.config.Config;
import minegame159.meteorclient.utils.network.OnlinePlayers;
import net.minecraft.client.gui.screen.Screen;

public class ConfigTab extends Tab {
    public ConfigTab() {
        super("Config");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new ConfigScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof ConfigScreen;
    }

    private static class ConfigScreen extends WindowTabScreen {
        public ConfigScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            Settings s = new Settings();

            SettingGroup sgGeneral = s.getDefaultGroup();

            // General

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
                        invalidate();
                    })
                    .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().customFont))
                    .build()
            );

            sgGeneral.add(new BoolSetting.Builder()
                    .name("rainbow-prefix")
                    .description("Makes the [Meteor] prefix on chat info rainbow.")
                    .defaultValue(true)
                    .onChanged(aBoolean -> Config.get().rainbowPrefix = aBoolean)
                    .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().rainbowPrefix))
                    .build()
            );

            sgGeneral.add(new DoubleSetting.Builder()
                    .name("rainbow-prefix-speed")
                    .description("The speed of the rainbow prefix cycle.")
                    .defaultValue(0.005)
                    .onChanged(value -> Config.get().rainbowPrefixSpeed = value)
                    .onModuleActivated(setting -> setting.set(Config.get().rainbowPrefixSpeed))
                    .build()
            );

            sgGeneral.add(new DoubleSetting.Builder()
                    .name("rainbow-prefix-spread")
                    .description("The spread of the rainbow prefix cycle.")
                    .defaultValue(0.02)
                    .onChanged(value -> Config.get().rainbowPrefixSpread = value)
                    .onModuleActivated(setting -> setting.set(Config.get().rainbowPrefixSpread))
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

            s.onActivated();
            add(theme.settings(s)).expandX();
        }
    }
}
