/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.tabs.builtin;

import motordevelopment.motorclient.gui.GuiTheme;
import motordevelopment.motorclient.gui.renderer.GuiRenderer;
import motordevelopment.motorclient.gui.tabs.Tab;
import motordevelopment.motorclient.gui.tabs.TabScreen;
import motordevelopment.motorclient.gui.tabs.WindowTabScreen;
import motordevelopment.motorclient.gui.widgets.containers.WHorizontalList;
import motordevelopment.motorclient.gui.widgets.pressable.WButton;
import motordevelopment.motorclient.gui.widgets.pressable.WCheckbox;
import motordevelopment.motorclient.systems.hud.Hud;
import motordevelopment.motorclient.systems.hud.screens.HudEditorScreen;
import motordevelopment.motorclient.utils.misc.NbtUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;

import static motordevelopment.motorclient.MotorClient.mc;

public class HudTab extends Tab {
    public HudTab() {
        super("HUD");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new HudScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof HudScreen;
    }

    public static class HudScreen extends WindowTabScreen {
        private final Hud hud;

        public HudScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            hud = Hud.get();
            hud.settings.onActivated();
        }

        @Override
        public void initWidgets() {
            add(theme.settings(hud.settings)).expandX();

            add(theme.horizontalSeparator()).expandX();

            WButton openEditor = add(theme.button("Edit")).expandX().widget();
            openEditor.action = () -> mc.setScreen(new HudEditorScreen(theme));

            WHorizontalList buttons = add(theme.horizontalList()).expandX().widget();
            buttons.add(theme.button("Clear")).expandX().widget().action = hud::clear;
            buttons.add(theme.button("Reset to default elements")).expandX().widget().action = hud::resetToDefaultElements;

            add(theme.horizontalSeparator()).expandX();

            WHorizontalList bottom = add(theme.horizontalList()).expandX().widget();

            bottom.add(theme.label("Active: "));
            WCheckbox active = bottom.add(theme.checkbox(hud.active)).expandCellX().widget();
            active.action = () -> hud.active = active.checked;

            WButton resetSettings = bottom.add(theme.button(GuiRenderer.RESET)).widget();
            resetSettings.action = hud.settings::reset;
        }

        @Override
        protected void onRenderBefore(DrawContext drawContext, float delta) {
            HudEditorScreen.renderElements(drawContext);
        }

        @Override
        public boolean toClipboard() {
            return NbtUtils.toClipboard("hud-settings", hud.settings.toTag());
        }

        @Override
        public boolean fromClipboard() {
            NbtCompound clipboard = NbtUtils.fromClipboard(hud.settings.toTag());

            if (clipboard != null) {
                hud.settings.fromTag(clipboard);
                return true;
            }

            return false;
        }
    }
}
