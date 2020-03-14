package minegame159.meteorclient.gui.clickgui;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.ModuleBindChangedEvent;
import minegame159.meteorclient.gui.PanelListScreen;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Color;
import org.lwjgl.glfw.GLFW;

public class ModuleScreen extends PanelListScreen implements Listenable {
    private Module module;

    private WLabel bindLabel;
    private boolean canResetBind = true;

    public ModuleScreen(Module module) {
        super(module.title);
        this.module = module;

        // Description
        add(new WLabel(module.description));
        add(new WHorizontalSeparator());

        // Settings
        WGrid grid = add(new WGrid(4, 4, 3));
        for (Setting setting : module.settings) {
            WLabel name = new WLabel(setting.title + ":");
            name.tooltip = setting.description;

            WWidget s;
            if (setting.get() instanceof Boolean) {
                s = new WCheckbox((boolean) setting.get());
                ((WCheckbox) s).setAction(wCheckbox -> setting.set(wCheckbox.checked));
            }
            else if (setting.get() instanceof Integer) {
                s = new WIntTextBox((int) setting.get(), 9);
                ((WIntTextBox) s).action = wIntTextBox -> setting.set(wIntTextBox.value);
            }
            else if (setting.get() instanceof Double) {
                s = new WDoubleTextBox((double) setting.get(), 9);
                ((WDoubleTextBox) s).action = wDoubleTextBox -> setting.set(wDoubleTextBox.value);
            }
            else if (setting.get() instanceof Enum) {
                s = new WEnumButton<>((Enum<?>) setting.get());
                ((WEnumButton) s).action = o -> setting.set(((WEnumButton) o).value);
            }
            else if (setting.get() instanceof Color) {
                s = new WColorEdit((Color) setting.get());
                ((WColorEdit) s).action = wColorEdit -> setting.set(wColorEdit.color);
            } else s = new WLabel("Setting type not supported.");
            s.tooltip = setting.description;

            WButton reset = new WButton("Reset");
            reset.action = () -> {
                setting.reset();
                if (s instanceof WCheckbox) ((WCheckbox) s).checked = (boolean) setting.get();
                else if (s instanceof WIntTextBox) ((WIntTextBox) s).setValue((Integer) setting.get());
                else if (s instanceof WDoubleTextBox) ((WDoubleTextBox) s).setValue((Double) setting.get());
                else if (s instanceof WEnumButton) ((WEnumButton) s).setValue((Enum<?>) setting.get());
                else if (s instanceof WColorEdit) ((WColorEdit) s).set((Color) setting.get());
            };

            grid.addRow(name, s, reset);
        }
        if (module.settings.size() > 0 && !module.setting) add(new WHorizontalSeparator());

        if (!module.setting) {
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
            active.add(new WCheckbox(module.isActive())).setAction(wCheckbox -> module.toggle());
        }

        layout();
        MeteorClient.eventBus.subscribe(this);
    }

    @EventHandler
    private Listener<ModuleBindChangedEvent> onModuleBindChanged = new Listener<>(event -> {
        if (event.module == module) {
            canResetBind = true;
            bindLabel.text = getBindLabelText();
            layout();
        }
    });

    private String getBindLabelText() {
        return "Bind: " + (module.getKey() == -1 ? "none" : GLFW.glfwGetKeyName(module.getKey(), 0));
    }

    @Override
    public void onClose() {
        MeteorClient.eventBus.unsubscribe(this);
        super.onClose();
    }
}
