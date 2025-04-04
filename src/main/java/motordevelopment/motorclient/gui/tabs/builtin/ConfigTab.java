/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.tabs.builtin;

import motordevelopment.motorclient.gui.GuiTheme;
import motordevelopment.motorclient.gui.tabs.Tab;
import motordevelopment.motorclient.gui.tabs.TabScreen;
import motordevelopment.motorclient.gui.tabs.WindowTabScreen;
import motordevelopment.motorclient.settings.Settings;
import motordevelopment.motorclient.systems.config.Config;
import motordevelopment.motorclient.utils.misc.NbtUtils;
import motordevelopment.motorclient.utils.render.prompts.YesNoPrompt;
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
                        .title("Empty command prefix")
                        .message("You have set your command prefix to nothing.")
                        .message("This WILL prevent you from sending chat messages.")
                        .message("Do you want to reset your prefix back to '.'?")
                        .onYes(() -> Config.get().prefix.set("."))
                        .id("empty-command-prefix")
                        .show();
                }
                else if (prefix.equals("/")) {
                    YesNoPrompt.create(theme, this.parent)
                        .title("Potential prefix conflict")
                        .message("You have set your command prefix to '/', which is used by minecraft.")
                        .message("This can cause conflict issues between motor and minecraft commands.")
                        .message("Do you want to reset your prefix to '.'?")
                        .onYes(() -> Config.get().prefix.set("."))
                        .id("minecraft-prefix-conflict")
                        .show();
                }
                else if (prefix.length() > 7) {
                    YesNoPrompt.create(theme, this.parent)
                        .title("Long command prefix")
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
