package minegame159.meteorclient.gui.screens;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.ModuleBindChangedEvent;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import org.lwjgl.glfw.GLFW;

public class ModuleScreen extends WindowScreen implements Listenable {
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
            add(module.settings.createTable()).fillX().expandX().getWidget();
        } else {
            add(new WHorizontalSeparator()).fillX().expandX();
            row();
        }

        WWidget customWidget = module.getWidget();
        if (customWidget != null) {
            if (module.settings.sizeGroups() > 0) {
                add(new WHorizontalSeparator()).fillX().expandX();
                row();
            }

            add(customWidget);
            row();
        }

        if (module instanceof ToggleModule) {
            if (customWidget != null || module.settings.sizeGroups() > 0) {
                add(new WHorizontalSeparator()).fillX().expandX();
                row();
            }

            // Bind
            WTable bindList = add(new WTable()).fillX().expandX().getWidget();
            bindLabel = bindList.add(new WLabel(getBindLabelText())).getWidget();
            bindList.add(new WButton("Set bind")).getWidget().action = button -> {
                ModuleManager.INSTANCE.setModuleToBind(module);
                canResetBind = false;
                bindLabel.setText("Bind: press any key");
            };
            bindList.add(new WButton("Reset bind")).getWidget().action = button -> {
                if (canResetBind) {
                    module.setKey(-1);
                    bindLabel.setText(getBindLabelText());
                }
            };
            row();

            add(new WHorizontalSeparator()).fillX().expandX();
            row();

            // Active
            WTable activeList = add(new WTable()).fillX().expandX().getWidget();
            activeList.add(new WLabel("Active:"));
            activeList.add(new WCheckbox(((ToggleModule) module).isActive())).getWidget().action = checkbox -> {
                if (((ToggleModule) module).isActive() != checkbox.checked) ((ToggleModule) module).toggle(mc.world != null);
            };
        }
    }

    @Override
    protected void init() {
        super.init();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    private Listener<ModuleBindChangedEvent> onModuleBindChanged = new Listener<>(event -> {
        if (event.module == module) {
            canResetBind = true;
            bindLabel.setText(getBindLabelText());
        }
    });

    private String getBindLabelText() {
        return "Bind: " + (module.getKey() == -1 ? "none" : GLFW.glfwGetKeyName(module.getKey(), 0));
    }

    @Override
    public void onClose() {
        MeteorClient.EVENT_BUS.unsubscribe(this);
        super.onClose();
    }
}
