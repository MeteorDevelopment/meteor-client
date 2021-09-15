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
import meteordevelopment.meteorclient.utils.render.prompts.YesNoPrompt;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.nbt.NbtCompound;

import static meteordevelopment.meteorclient.utils.Utils.mc;

public class ConfigTab extends Tab {
    private static final Settings settings = new Settings();

    private static final SettingGroup sgGeneral = settings.getDefaultGroup();
    private static final SettingGroup sgChat = settings.createGroup("Chat");
    private static final SettingGroup sgScreens = settings.createGroup("Screens");

    public static final Setting<Boolean> customFont = sgGeneral.add(new BoolSetting.Builder()
        .name("custom-font")
        .description("Use a custom font.")
        .defaultValue(true)
        .onChanged(aBoolean -> Config.get().customFont = aBoolean)
        .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().customFont))
        .build()
    );

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
        .visible(customFont::get)
        .build()
    );

    public static final Setting<Double> rainbowSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("rainbow-speed")
        .description("The global rainbow speed.")
        .min(0).max(10)
        .sliderMax(5)
        .defaultValue(0.5)
        .decimalPlaces(2)
        .onChanged(value -> RainbowColors.GLOBAL.setSpeed(value / 100))
        .onModuleActivated(setting -> setting.set(RainbowColors.GLOBAL.getSpeed() * 100))
        .build()
    );

    public static final Setting<Integer> rotationHoldTicks = sgGeneral.add(new IntSetting.Builder()
        .name("rotation-hold")
        .description("Hold long to hold server side rotation when not sending any packets.")
        .defaultValue(4)
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

    public static final Setting<Boolean> openChatOnPrefix = sgChat.add(new BoolSetting.Builder()
        .name("open-chat-on-prefix")
        .description("Open chat when command prefix is pressed. Works like pressing '/' in vanilla.")
        .defaultValue(true)
        .onChanged(aBoolean -> Config.get().openChatOnPrefix = aBoolean)
        .onModuleActivated(booleanSetting -> booleanSetting.set(Config.get().openChatOnPrefix))
        .build()
    );

    public static final Setting<Boolean> chatCommandsInfo = sgChat.add(new BoolSetting.Builder()
        .name("chat-commands-info")
        .description("Sends a chat message when you use chat commands (eg toggling module, changing a setting, etc).")
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
        .visible(chatCommandsInfo::get)
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

    public static final Setting<Boolean> titleScreenSplashes = sgScreens.add(new BoolSetting.Builder()
        .name("title-screen-splashes")
        .description("Show Meteor splash texts on title screen")
        .defaultValue(true)
        .onChanged(aBool -> Config.get().titleScreenSplashes = aBool)
        .onModuleActivated(boolSetting -> boolSetting.set(Config.get().titleScreenSplashes))
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
        .visible(customWindowTitle::get)
        .build()
    );

    public static final Setting<Boolean> useTeamColor = sgGeneral.add(new BoolSetting.Builder()
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

            onClosed(() -> {
                String prefix = Config.get().prefix;

                if (prefix.isBlank()) {
                    YesNoPrompt.create(theme, this.parent)
                        .title("Empty command prefix")
                        .message("You have set your command prefix to nothing.")
                        .message("This WILL prevent you from sending chat messages.")
                        .message("Do you want to reset your prefix back to '.'?")
                        .onYes(() -> Config.get().prefix = ".")
                        .id("empty-command-prefix")
                        .show();
                }
                else if (prefix.equals("/")) {
                    YesNoPrompt.create(theme, this.parent)
                        .title("Potential prefix conflict")
                        .message("You have set your command prefix to '/', which is used by minecraft.")
                        .message("This can cause conflict issues between meteor and minecraft commands.")
                        .message("Do you want to reset your prefix to '.'?")
                        .onYes(() -> Config.get().prefix = ".")
                        .id("minecraft-prefix-conflict")
                        .show();
                }
                else if (prefix.length() > 7) {
                    YesNoPrompt.create(theme, this.parent)
                        .title("Long command prefix")
                        .message("You have set your command prefix to a very long string.")
                        .message("This means that in order to execute any command, you will need to type %s followed by the command you want to run.", prefix)
                        .message("Do you want to reset your prefix back to '.'?")
                        .onYes(() -> Config.get().prefix = ".")
                        .id("long-command-prefix")
                        .show();
                }
                else if (isUsedKey()) {
                    YesNoPrompt.create(theme, this.parent)
                        .title("Prefix keybind")
                        .message("You have \"Open Chat On Prefix\" setting enabled and your command prefix has a conflict with another keybind.")
                        .message("Do you want to disable \"Open Chat On Prefix\" setting?")
                        .onYes(() -> Config.get().openChatOnPrefix = false)
                        .id("prefix-keybind")
                        .show();
                }
            });
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
            NbtCompound clipboard = NbtUtils.fromClipboard(Config.get().toTag());

            if (clipboard != null) {
                Config.get().fromTag(clipboard);
                return true;
            }

            return false;
        }
    }

    private static boolean isUsedKey() {
        if (!Config.get().openChatOnPrefix) return false;

        String prefixKeybindTranslation = String.format("key.keyboard.%s",  Config.get().prefix.toLowerCase().substring(0,1));
        for (KeyBinding key: mc.options.keysAll) {
            if (key.getBoundKeyTranslationKey().equals(prefixKeybindTranslation)) return true;
        }

        return false;
    }
}
