/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.meteor.ModuleBindChangedEvent;
import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.WindowScreen;
import minegame159.meteorclient.gui.utils.Cell;
import minegame159.meteorclient.gui.widgets.WKeybind;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.gui.widgets.containers.WContainer;
import minegame159.meteorclient.gui.widgets.containers.WHorizontalList;
import minegame159.meteorclient.gui.widgets.pressable.WCheckbox;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.utils.Utils;

import static minegame159.meteorclient.utils.Utils.getWindowWidth;

public class ModuleScreen extends WindowScreen {
    private final WKeybind keybind;

    public ModuleScreen(GuiTheme theme, Module module) {
        super(theme, module.title);

        // Description
        add(theme.label(module.description, getWindowWidth() / 2.0));

        // Settings
        if (module.settings.groups.size() > 0) {
            add(theme.settings(module.settings)).expandX();
        }

        add(theme.horizontalSeparator()).expandX();

        // Custom widget
        WWidget widget = module.getWidget(theme);
        if (widget != null) {
            Cell<WWidget> cell = add(widget);
            if (widget instanceof WContainer) cell.expandX();

            add(theme.horizontalSeparator()).expandX();
        }

        // Bind
        keybind = add(theme.keybind(module.keybind, true)).widget();
        keybind.actionOnSet = () -> Modules.get().setModuleToBind(module);

        // Toggle on bind release
        WHorizontalList tobr = add(theme.horizontalList()).widget();

        tobr.add(theme.label("Toggle on bind release: "));
        WCheckbox tobrC = tobr.add(theme.checkbox(module.toggleOnBindRelease)).widget();
        tobrC.action = () -> module.toggleOnBindRelease = tobrC.checked;

        add(theme.horizontalSeparator()).expandX();

        // Bottom
        WHorizontalList bottom = add(theme.horizontalList()).expandX().widget();

        //   Active
        bottom.add(theme.label("Active: "));
        WCheckbox active = bottom.add(theme.checkbox(module.isActive())).expandCellX().widget();
        active.action = () -> {
            if (module.isActive() != active.checked) module.toggle(Utils.canUpdate());
        };

        //   Visible
        bottom.add(theme.label("Visible: "));
        WCheckbox visible = bottom.add(theme.checkbox(module.isVisible())).widget();
        visible.action = () -> {
            if (module.isVisible() != visible.checked) module.setVisible(visible.checked);
        };
    }

    @EventHandler
    private void onModuleBindChanged(ModuleBindChangedEvent event) {
        keybind.reset();
    }
}
