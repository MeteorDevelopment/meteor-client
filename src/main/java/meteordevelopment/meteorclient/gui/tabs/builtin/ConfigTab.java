/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.tabs.builtin;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.prompts.YesNoPrompt;
import net.minecraft.client.gui.screen.Screen;

public class ConfigTab extends Tab {
    public ConfigTab() {
        super("配置");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new ConfigScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof ConfigScreen;
    }

    public static class ConfigScreen extends WindowTabScreen {
        private final Settings settings;

        public ConfigScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            settings = Config.get().settings;
            settings.onActivated();

            onClosed(() -> {
                String prefix = Config.get().prefix.get();

                if (prefix.isBlank()) {
                    YesNoPrompt.create(theme, this.parent)
                        .title("空命令前缀")
                        .message("您已将命令前缀设置为空.")
                        .message("这将阻止您发送聊天消息.")
                        .message("是否要将前缀重置为“.”?")
                        .onYes(() -> Config.get().prefix.set("."))
                        .id("空命令前缀")
                        .show();
                }
                else if (prefix.equals("/")) {
                    YesNoPrompt.create(theme, this.parent)
                        .title("潜在的前缀冲突")
                        .message("您已将命令前缀设置为 minecraft 使用的“/”.")
                        .message("这可能会导致流星和我的世界命令之间的冲突问题.")
                        .message("是否要将前缀重置为“.”?")
                        .onYes(() -> Config.get().prefix.set("."))
                        .id("我的世界前缀冲突")
                        .show();
                }
                else if (prefix.length() > 7) {
                    YesNoPrompt.create(theme, this.parent)
                        .title("长命令前缀")
                        .message("You have set your command prefix to a very long string.")
                        .message("This means that in order to execute any command, you will need to type %s followed by the command you want to run.", prefix)
                        .message("Do you want to reset your prefix back to '.'?")
                        .onYes(() -> Config.get().prefix.set("."))
                        .id("long-command-prefix")
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
            return NbtUtils.fromClipboard(Config.get());
        }
    }
}
