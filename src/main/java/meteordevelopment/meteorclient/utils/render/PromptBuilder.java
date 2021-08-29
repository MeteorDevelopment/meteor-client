/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.utils.Utils.mc;

public class PromptBuilder {
    private final GuiTheme theme;
    private final Screen parent;
    private String title = "";
    private final List<String> messages = new ArrayList<>();
    private Runnable onYes = () -> {};
    private Runnable onNo = () -> {};
    private String promptId = null;

    public PromptBuilder() {
        this(GuiThemes.get(), mc.currentScreen);
    }

    public PromptBuilder(GuiTheme theme, Screen parent) {
        this.theme = theme;
        this.parent = parent;
    }

    public PromptBuilder title(String title) {
        this.title = title;
        return this;
    }

    public PromptBuilder message(String message) {
        this.messages.add(message);
        return this;
    }

    public PromptBuilder message(String message, Object... args) {
        this.messages.add(String.format(message, args));
        return this;
    }

    public PromptBuilder onYes(Runnable runnable) {
        this.onYes = runnable;
        return this;
    }

    public PromptBuilder onNo(Runnable runnable) {
        this.onNo = runnable;
        return this;
    }

    public PromptBuilder promptId(String from) {
        this.promptId = from;
        return this;
    }

    public void show() {
        if (promptId == null) this.promptId(this.title);

        if (Config.get().dontShowAgainPrompts.contains(promptId)) {
            onNo.run();
            return;
        }

        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                Screen prompt = new PromptScreen(theme);
                mc.setScreen(prompt);
            });
        }
        else {
            Screen prompt = new PromptScreen(theme);
            mc.setScreen(prompt);
        }
    }

    private class PromptScreen extends WindowScreen {
        public PromptScreen(GuiTheme theme) {
            super(theme, PromptBuilder.this.title);
            this.parent = PromptBuilder.this.parent;

            for (String line : messages) {
                add(theme.label(line)).expandX();
            }

            add(theme.horizontalSeparator()).expandX();

            WHorizontalList checkboxContainer = add(theme.horizontalList()).expandX().widget();
            WCheckbox dontShowAgainCheckbox = checkboxContainer.add(theme.checkbox(false)).widget();
            checkboxContainer.add(theme.label("Don't show this prompt again.")).expandX();

            WHorizontalList list = add(theme.horizontalList()).expandX().widget();

            WButton yesButton = list.add(theme.button("Yes")).expandX().widget();
            yesButton.action = () -> {
                onYes.run();
                this.onClose();
            };

            WButton noButton = list.add(theme.button("No")).expandX().widget();
            noButton.action = () -> {
                onNo.run();
                if (dontShowAgainCheckbox.checked)
                    Config.get().dontShowAgainPrompts.add(promptId);
                this.onClose();
            };

            dontShowAgainCheckbox.action = () -> {
                yesButton.visible = !dontShowAgainCheckbox.checked;
            };
        }
    }
}
