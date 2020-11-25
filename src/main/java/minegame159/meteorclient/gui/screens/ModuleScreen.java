/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.screens;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.ModuleBindChangedEvent;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;

public class ModuleScreen extends WindowScreen {
    private Module module;

    private WLabel bindLabel;
    private boolean canResetBind = true;

    public ModuleScreen(Module module) {
        super(module.title, true);
        this.module = module;

        initWidgets();
    }

    private void initWidgets() {
        // Description
        add(new WLabel(module.description));
        row();

        if (module.settings.sizeGroups() > 0) {
            add(module.settings.createTable(false)).fillX().expandX().getWidget();
        } else {
            add(new WHorizontalSeparator());
        }

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

        if (module instanceof ToggleModule) {
            if (customWidget != null || module.settings.sizeGroups() > 0) {
                row();
                add(new WHorizontalSeparator());
            }

            // Bind
            WTable bindList = add(new WTable()).fillX().expandX().getWidget();
            bindLabel = bindList.add(new WLabel(getBindLabelText())).getWidget();
            bindList.add(new WButton("Set bind")).getWidget().action = () -> {
                ModuleManager.INSTANCE.setModuleToBind(module);
                canResetBind = false;
                bindLabel.setText("Bind: press any key");
            };
            bindList.add(new WButton("Reset bind")).getWidget().action = () -> {
                if (canResetBind) {
                    module.setKey(-1);
                    bindLabel.setText(getBindLabelText());
                }
            };
            row();

            // Toggle on key release
            WTable tokrTable = add(new WTable()).fillX().expandX().getWidget();
            tokrTable.add(new WLabel("Toggle on key release:"));
            WCheckbox toggleOnKeyRelease = tokrTable.add(new WCheckbox(module.toggleOnKeyRelease)).getWidget();
            toggleOnKeyRelease.action = () -> {
                module.toggleOnKeyRelease = toggleOnKeyRelease.checked;
                ModuleManager.INSTANCE.save();
            };
            row();

            add(new WHorizontalSeparator());

            // Bottom
            WTable bottomTable = add(new WTable()).fillX().expandX().getWidget();

            //   Active
            bottomTable.add(new WLabel("Active:"));
            WCheckbox active = bottomTable.add(new WCheckbox(((ToggleModule) module).isActive())).getWidget();
            active.action = () -> {
                if (((ToggleModule) module).isActive() != active.checked) ((ToggleModule) module).toggle(MinecraftClient.getInstance().world != null);
            };

            //   Visible
            bottomTable.add(new WLabel("Visible: ")).fillX().right().getWidget().tooltip = "Visible in HUD.";
            WCheckbox visibleCheckbox = bottomTable.add(new WCheckbox(((ToggleModule) module).isVisible())).getWidget();
            visibleCheckbox.tooltip = "Visible in HUD.";
            visibleCheckbox.action = () -> {
                if (((ToggleModule) module).isVisible() != visibleCheckbox.checked) ((ToggleModule) module).setVisible(visibleCheckbox.checked);
            };
        }
    }

    @EventHandler
    private final Listener<ModuleBindChangedEvent> onModuleBindChanged = new Listener<>(event -> {
        if (event.module == module) {
            canResetBind = true;
            bindLabel.setText(getBindLabelText());
        }
    });

    private String getBindLabelText() {
        return "Bind: " + (module.getKey() == -1 ? "none" :  Utils.getKeyName(module.getKey()));
    }
}
