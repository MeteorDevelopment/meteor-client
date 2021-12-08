/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.builtin;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ConfigTab extends Tab {
    private static final Settings settings = new Settings();

    private static final SettingGroup sgVisual = settings.createGroup("Visual");
    private static final SettingGroup sgChat = settings.createGroup("Chat");
    private static final SettingGroup sgMisc = settings.createGroup("Misc");

    // Visual

    public static final Setting<Boolean> customFont = sgVisual.add(new BoolSetting.Builder()
        .name("custom-font")
        .description("Use a custom font.")
        .defaultValue(true)
        .onChanged(aBoolean -> Config.get().customFont = aBoolean)
        .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().customFont))
        .build()
    );

    public static final Setting<String> font = sgVisual.add(new ProvidedStringSetting.Builder()
        .name("font")
        .description("Custom font to use (picked from .minecraft/meteor-client/fonts folder).")
        .supplier(Fonts::getAvailableFonts)
        .defaultValue(Fonts.DEFAULT_FONT)
        .onChanged(s -> {
            Config.get().font = s;
            Fonts.load();
        })
        .onModuleActivated(stringSetting -> stringSetting.set(Config.get().font))
        .visible(customFont::get)
        .build()
    );

    public static final Setting<Double> rainbowSpeed = sgVisual.add(new DoubleSetting.Builder()
        .name("rainbow-speed")
        .description("The global rainbow speed.")
        .defaultValue(0.5)
        .range(0, 10)
        .sliderMax(5)
        .onChanged(value -> RainbowColors.GLOBAL.setSpeed(value / 100))
        .onModuleActivated(doubleSetting -> doubleSetting.set(Config.get().rainbowSpeed))
        .build()
    );

    // Chat

    public static final Setting<Boolean> chatFeedback = sgChat.add(new BoolSetting.Builder()
        .name("chat-feedback")
        .description("Sends chat feedback when meteor performs certain actions.")
        .defaultValue(true)
        .onChanged(aBoolean -> Config.get().chatFeedback = aBoolean)
        .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().chatFeedback))
        .build()
    );

    public static final Setting<Boolean> deleteChatFeedback = sgChat.add(new BoolSetting.Builder()
        .name("delete-chat-feedback")
        .description("Delete previous matching chat feedback to keep chat clear.")
        .visible(chatFeedback::get)
        .defaultValue(true)
        .onChanged(aBoolean -> Config.get().deleteChatFeedback = aBoolean)
        .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().deleteChatFeedback))
        .build()
    );

    // Misc

    public static final Setting<Integer> rotationHoldTicks = sgMisc.add(new IntSetting.Builder()
        .name("rotation-hold")
        .description("Hold long to hold server side rotation when not sending any packets.")
        .defaultValue(4)
        .onChanged(integer -> Config.get().rotationHoldTicks = integer)
        .onModuleActivated(integerSetting -> integerSetting.set(Config.get().rotationHoldTicks))
        .build()
    );

    public static final Setting<Boolean> useTeamColor = sgMisc.add(new BoolSetting.Builder()
        .name("use-team-color")
        .description("Uses player's team color for rendering things like esp and tracers.")
        .defaultValue(true)
        .onChanged(aBoolean -> Config.get().useTeamColor = aBoolean)
        .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().useTeamColor))
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
        }

        @Override
        public void initWidgets() {
            add(theme.settings(settings)).expandX();
        }

        @Override
        public void tick() {
            super.tick();

            settings.tick(window, theme);
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard(Config.get());
        }

        @Override
        public boolean fromClipboard() {
            return NbtUtils.fromClipboard(Config.get());
        }
    }
}
