package minegame159.meteorclient.gui.clickgui;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.ModuleBindChangedEvent;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.Setting;
import org.lwjgl.glfw.GLFW;

public class ModuleScreen extends WindowScreen implements Listenable {
    private Module module;

    private WLabel bindLabel;
    private boolean canResetBind = true;

    public ModuleScreen(Module module) {
        super(module.title);
        this.module = module;

        // Description
        add(new WLabel(module.description));
        if (module.settingGroups.size() <= 1) add(new WHorizontalSeparator());

        // Settings
        for (String group : module.settingGroups.keySet()) {
            if (module.settingGroups.size() > 1) add(new WHorizontalSeparator(group));

            WGrid grid = add(new WGrid(4, 4, 3));
            for (Setting<?> setting : module.settingGroups.get(group)) generateSettingToGrid(grid, setting);
        }

        WWidget customWidget = module.getWidget();
        if (customWidget != null) {
            if (module.settings.size() > 0) add(new WHorizontalSeparator());
            add(customWidget);
        }

        if (module instanceof ToggleModule) {
            if (customWidget != null || module.settings.size() > 0) add(new WHorizontalSeparator());

            // Bind
            WHorizontalList bind = add(new WHorizontalList(4));
            bindLabel = bind.add(new WLabel(getBindLabelText()));
            bind.add(new WButton("Set bind")).action = () -> {
                ModuleManager.INSTANCE.setModuleToBind(module);
                canResetBind = false;
                bindLabel.text = "Bind: press any key";
                layout();
            };
            bind.add(new WButton("Reset bind")).action = () -> {
                if (canResetBind) {
                    module.setKey(-1);
                    bindLabel.text = getBindLabelText();
                    layout();
                }
            };
            add(new WHorizontalSeparator());

            // Active
            WHorizontalList active = add(new WHorizontalList(4));
            active.add(new WLabel("Active:"));
            active.add(new WCheckbox(((ToggleModule) module).isActive())).setAction(wCheckbox -> {
                if (((ToggleModule) module).isActive() != wCheckbox.checked) ((ToggleModule) module).toggle(mc.world != null);
            });
        }

        layout();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    private Listener<ModuleBindChangedEvent> onModuleBindChanged = new Listener<>(event -> {
        if (event.module == module) {
            canResetBind = true;
            bindLabel.text = getBindLabelText();
            layout();
        }
    });

    private void generateSettingToGrid(WGrid grid, Setting<?> setting) {
        WLabel name = new WLabel(setting.title + ":");
        name.tooltip = setting.description;

        WWidget s = setting.widget;
        s.tooltip = setting.description;

        WButton reset = new WButton("Reset");
        reset.action = setting::reset;

        grid.addRow(name, s, reset);
    }

    private String getBindLabelText() {
        return "Bind: " + (module.getKey() == -1 ? "none" : GLFW.glfwGetKeyName(module.getKey(), 0));
    }

    @Override
    public void onClose() {
        MeteorClient.EVENT_BUS.unsubscribe(this);
        super.onClose();
    }
}
