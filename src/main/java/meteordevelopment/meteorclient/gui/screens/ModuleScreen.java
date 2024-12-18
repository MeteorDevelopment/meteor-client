/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.ActiveModulesChangedEvent;
import meteordevelopment.meteorclient.events.meteor.ModuleBindChangedEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WFavorite;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;

import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class ModuleScreen extends WindowScreen {
    private final Module module;

    private WContainer settingsContainer;
    private WKeybind keybind;
    private WCheckbox active;

    public ModuleScreen(GuiTheme theme, Module module) {
        super(theme, theme.favorite(module.favorite), module.title);
        ((WFavorite) window.icon).action = () -> module.favorite = ((WFavorite) window.icon).checked;

        this.module = module;
    }

    @Override
    public void initWidgets() {
        // Description
        add(theme.label(module.description, getWindowWidth() / 2.0));

        // Settings
        if (!module.settings.groups.isEmpty()) {
            settingsContainer = add(theme.verticalList()).expandX().widget();
            settingsContainer.add(theme.settings(module.settings)).expandX();
        }

        // Custom widget
        WWidget widget = module.getWidget(theme);

        if (widget != null) {
            add(theme.horizontalSeparator()).expandX();
            Cell<WWidget> cell = add(widget);
            if (widget instanceof WContainer) cell.expandX();
        }

        // Bind
        WSection section = add(theme.section("Bind", true)).expandX().widget();

        // Keybind
        WHorizontalList bind = section.add(theme.horizontalList()).expandX().widget();

        bind.add(theme.label("Bind: "));
        keybind = bind.add(theme.keybind(module.keybind)).expandX().widget();
        keybind.actionOnSet = () -> Modules.get().setModuleToBind(module);

        WButton reset = bind.add(theme.button(GuiRenderer.RESET)).expandCellX().right().widget();
        reset.action = keybind::resetBind;

        // Toggle on bind release
        WHorizontalList tobr = section.add(theme.horizontalList()).widget();

        tobr.add(theme.label("Toggle on bind release: "));
        WCheckbox tobrC = tobr.add(theme.checkbox(module.toggleOnBindRelease)).widget();
        tobrC.action = () -> module.toggleOnBindRelease = tobrC.checked;

        // Chat feedback
        WHorizontalList cf = section.add(theme.horizontalList()).widget();

        cf.add(theme.label("Chat Feedback: "));
        WCheckbox cfC = cf.add(theme.checkbox(module.chatFeedback)).widget();
        cfC.action = () -> module.chatFeedback = cfC.checked;

        add(theme.horizontalSeparator()).expandX();

        // Bottom
        WHorizontalList bottom = add(theme.horizontalList()).expandX().widget();

        // Active
        bottom.add(theme.label("Active: "));
        active = bottom.add(theme.checkbox(module.isActive())).expandCellX().widget();
        active.action = () -> {
            if (module.isActive() != active.checked) module.toggle();
        };

        if (module.addon != null && module.addon != MeteorClient.ADDON) {
            bottom.add(theme.label("From: ")).right().widget();
            bottom.add(theme.label(module.addon.name).color(theme.textSecondaryColor())).right().widget();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !Modules.get().isBinding();
    }

    @Override
    public void tick() {
        super.tick();

        module.settings.tick(settingsContainer, theme);
    }

    @EventHandler
    private void onModuleBindChanged(ModuleBindChangedEvent event) {
        keybind.reset();
    }

    @EventHandler
    private void onActiveModulesChanged(ActiveModulesChangedEvent event) {
        this.active.checked = module.isActive();
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(module.title, module.toTag());
    }

    @Override
    public boolean fromClipboard() {
        NbtCompound clipboard = NbtUtils.fromClipboard(module.toTag());

        if (clipboard != null) {
            module.fromTag(clipboard);
            return true;
        }

        return false;
    }
}
