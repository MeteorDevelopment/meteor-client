/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.tabs.builtin;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.tabs.Tab;
import minegame159.meteorclient.gui.tabs.TabScreen;
import minegame159.meteorclient.gui.tabs.WindowTabScreen;
import minegame159.meteorclient.rendering.Fonts;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.config.Config;
import minegame159.meteorclient.utils.network.OnlinePlayers;
import minegame159.meteorclient.utils.render.color.RainbowColors;
import net.minecraft.client.gui.screen.Screen;

public class ConfigTab extends Tab {
    private static final Settings settings = new Settings();
    private static final SettingGroup sgGeneral = settings.getDefaultGroup();
    private static final SettingGroup sgChat = settings.createGroup("Chat");
    private static final SettingGroup sgScreens = settings.createGroup("Screens");

    public static final Setting<String> font = sgGeneral.add(new ProvidedStringSetting.Builder()
            .name("font")
            .description("Custom font to use (picked from .minecraft/meteor-client/fonts folder).")
            .supplier(Fonts::getAvailableFonts)
            .defaultValue(Fonts.DEFAULT_FONT)
            .onChanged(s -> {
                Config.get().font = s;
                Fonts.load();
            })
            .onModuleActivated(stringSetting -> stringSetting.set(Config.get().font))
            .build()
    );

    public static final Setting<Boolean> customFont = sgGeneral.add(new BoolSetting.Builder()
            .name("custom-font")
            .description("Use a custom font.")
            .defaultValue(true)
            .onChanged(aBoolean -> {
                Config.get().customFont = aBoolean;
                if (ConfigTab.currentScreen != null) ConfigTab.currentScreen.invalidate();
            })
            .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().customFont))
            .build()
    );

    public static final Setting<Double> rainbowSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("rainbow-speed")
            .description("The global rainbow speed.")
            .min(0)
            .sliderMax(5)
            .max(10)
            .defaultValue(0.5)
            .decimalPlaces(2)
            .onChanged(value -> RainbowColors.GLOBAL.setSpeed(value / 100))
            .onModuleActivated(setting -> setting.set(RainbowColors.GLOBAL.getSpeed() * 100))
            .build()
    );

    public static final Setting<Boolean> sendDataToApi = sgGeneral.add(new BoolSetting.Builder()
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

    public static final Setting<Integer> rotationHoldTicks = sgGeneral.add(new IntSetting.Builder()
            .name("rotation-hold-ticks")
            .description("Hold long to hold server side rotation when not sending any packets.")
            .defaultValue(9)
            .onChanged(integer -> Config.get().rotationHoldTicks = integer)
            .onModuleActivated(integerSetting -> integerSetting.set(Config.get().rotationHoldTicks))
            .build()
    );

    public static final Setting<String> prefix = sgChat.add(new StringSetting.Builder()
            .name("prefix")
            .description("Prefix.")
            .defaultValue(".")
            .onChanged(s -> Config.get().prefix = s)
            .onModuleActivated(stringSetting -> stringSetting.set(Config.get().prefix))
            .build()
    );

    public static final Setting<Boolean> chatCommandsInfo = sgChat.add(new BoolSetting.Builder()
            .name("chat-commands-info")
            .description("Sends a chat message when you use chat comamnds (eg toggling module, changing a setting, etc).")
            .defaultValue(true)
            .onChanged(aBoolean -> Config.get().chatCommandsInfo = aBoolean)
            .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().chatCommandsInfo))
            .build()
    );

    public static final Setting<Boolean> deleteChatCommandsInfo = sgChat.add(new BoolSetting.Builder()
            .name("delete-chat-commands-info")
            .description("Delete previous chat messages.")
            .defaultValue(true)
            .onChanged(aBoolean -> Config.get().deleteChatCommandsInfo = aBoolean)
            .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().deleteChatCommandsInfo))
            .build()
    );

    public static final Setting<Boolean> rainbowPrefix = sgChat.add(new BoolSetting.Builder()
            .name("rainbow-prefix")
            .description("Makes the [Meteor] prefix on chat info rainbow.")
            .defaultValue(true)
            .onChanged(aBoolean -> Config.get().rainbowPrefix = aBoolean)
            .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().rainbowPrefix))
            .build()
    );

    public static final Setting<Boolean> titleScreenCredits = sgScreens.add(new BoolSetting.Builder()
            .name("title-screen-credits")
            .description("Show Meteor credits on title screen")
            .defaultValue(true)
            .onChanged(aBool -> Config.get().titleScreenCredits = aBool)
            .onModuleActivated(boolSetting -> boolSetting.set(Config.get().titleScreenCredits))
            .build()
    );

    public static final Setting<Boolean> customWindowTitle = sgScreens.add(new BoolSetting.Builder()
            .name("custom-window-title")
            .description("Show custom text in the window title.")
            .defaultValue(false)
            .onChanged(aBool -> Config.get().customWindowTitle = aBool)
            .onModuleActivated(boolSetting -> boolSetting.set(Config.get().customWindowTitle))
            .build()
    );

    public static final Setting<String> customWindowTitleText = sgScreens.add(new StringSetting.Builder()
            .name("window-title-text")
            .description("The text it displays in the window title.")
            .defaultValue("Minecraft {mc_version} - Meteor Client {version}")
            .onChanged(titleText -> Config.get().customWindowTitleText = titleText)
            .onModuleActivated(stringSetting -> stringSetting.set(Config.get().customWindowTitleText))
            .build()
    );

    public static ConfigScreen currentScreen;

    public ConfigTab() {
        super("Config");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return currentScreen = new ConfigScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof ConfigScreen;
    }

    public static class ConfigScreen extends WindowTabScreen {
        public ConfigScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            settings.onActivated();
            add(theme.settings(settings)).expandX();
        }
    }
}
