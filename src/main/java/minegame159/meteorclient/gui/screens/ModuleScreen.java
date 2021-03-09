/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.meteor.ModuleBindChangedEvent;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.utils.Utils;

public class ModuleScreen extends WindowScreen {
    private final Module module;

    private WKeybind keybind;

    public ModuleScreen(Module module) {
        super(module.title, true);
        this.module = module;

        initWidgets();
    }

    private void initWidgets() {
        // Description
        add(new WLabel(module.description));
        row();

        // Settings
        if (module.settings.sizeGroups() > 0) {
            add(module.settings.createTable(false)).fillX().expandX().getWidget();
        }
        else {
            add(new WHorizontalSeparator());
        }

        // Custom widget
        WWidget customWidget = module.getWidget();
        if (customWidget != null) {
            if (module.settings.sizeGroups() > 0) {
                row();
                add(new WHorizontalSeparator());
            }

            Cell<WWidget> cell = add(customWidget);
            if (customWidget instanceof WTable) cell.fillX().expandX();
            row();
        }

        if (customWidget != null || module.settings.sizeGroups() > 0) {
            row();
            add(new WHorizontalSeparator());
        }

        // Bind
        keybind = add(new WKeybind(module.keybind)).getWidget();
        keybind.actionOnSet = () -> Modules.get().setModuleToBind(module);
        row();

        // Toggle on key release
        WTable tokrTable = add(new WTable()).fillX().expandX().getWidget();
        tokrTable.add(new WLabel("Toggle on key release:"));
        WCheckbox toggleOnKeyRelease = tokrTable.add(new WCheckbox(module.toggleOnKeyRelease)).getWidget();
        toggleOnKeyRelease.action = () -> module.toggleOnKeyRelease = toggleOnKeyRelease.checked;
        row();

        add(new WHorizontalSeparator());

        // Bottom
        WTable bottomTable = add(new WTable()).fillX().expandX().getWidget();

        //   Active
        bottomTable.add(new WLabel("Active:"));
        WCheckbox active = bottomTable.add(new WCheckbox(module.isActive())).getWidget();
        active.action = () -> {
            if (module.isActive() != active.checked) module.toggle(Utils.canUpdate());
        };

        //   Visible
        bottomTable.add(new WLabel("Visible: ")).fillX().right().getWidget().tooltip = "Shows the module in the array list.";
        WCheckbox visibleCheckbox = bottomTable.add(new WCheckbox(module.isVisible())).getWidget();
        visibleCheckbox.tooltip = "Shows the module in the array list.";
        visibleCheckbox.action = () -> {
            if (module.isVisible() != visibleCheckbox.checked) module.setVisible(visibleCheckbox.checked);
        };
    }

    @EventHandler
    private void onModuleBindChanged(ModuleBindChangedEvent event) {
        keybind.reset();
    }
}
