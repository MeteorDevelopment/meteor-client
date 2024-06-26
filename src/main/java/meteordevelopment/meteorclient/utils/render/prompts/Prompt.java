package meteordevelopment.meteorclient.utils.render.prompts;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@SuppressWarnings("unchecked") // cant instantiate a Prompt directly so this is fine
public abstract class Prompt<T> {
    final GuiTheme theme;
    final Screen parent;

    String title = "";
    final List<String> messages = new ArrayList<>();
    boolean dontShowAgainCheckboxVisible = true;
    String id = null;

    protected Prompt(GuiTheme theme, Screen parent) {
        this.theme = theme;
        this.parent = parent;
    }

    public T title(String title) {
        this.title = title;
        return (T) this;
    }

    public T message(String message) {
        this.messages.add(message);
        return (T) this;
    }

    public T message(String message, Object... args) {
        this.messages.add(String.format(message, args));
        return (T) this;
    }

    public T dontShowAgainCheckboxVisible(boolean visible) {
        this.dontShowAgainCheckboxVisible = visible;
        return (T) this;
    }

    public T id(String from) {
        this.id = from;
        return (T) this;
    }

    public boolean show() {
        if (id == null) this.id(this.title);
        if (Config.get().dontShowAgainPrompts.contains(id)) return false;

        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> mc.setScreen(new PromptScreen(theme)));
        }
        else {
            mc.setScreen(new PromptScreen(theme));
        }

        return true;
    }

    abstract void initialiseWidgets(PromptScreen screen);

    protected class PromptScreen extends WindowScreen {
        WCheckbox dontShowAgainCheckbox;
        WHorizontalList list;

        public PromptScreen(GuiTheme theme) {
            super(theme, Prompt.this.title);

            this.parent = Prompt.this.parent;
        }

        @Override
        public void initWidgets() {
            for (String line : messages) add(theme.label(line)).expandX();
            add(theme.horizontalSeparator()).expandX();

            if (dontShowAgainCheckboxVisible) {
                WHorizontalList checkboxContainer = add(theme.horizontalList()).expandX().widget();
                dontShowAgainCheckbox = checkboxContainer.add(theme.checkbox(false)).widget();
                checkboxContainer.add(theme.label("Don't show this again.")).expandX();
            } else dontShowAgainCheckbox = null;

            list = add(theme.horizontalList()).expandX().widget();

            initialiseWidgets(this);
        }
    }
}
