package minegame159.meteorclient.clickgui;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.clickgui.widgets.*;
import minegame159.meteorclient.events.ModuleBindChangedEvent;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Color;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

public class ModuleScreen extends WidgetScreen implements Listenable {
    private Screen parent;
    private Module module;
    private Label bindLabel;

    public ModuleScreen(Screen parent, Module module) {
        super(module.title);
        this.parent = parent;
        this.module = module;

        WindowCenter root = new WindowCenter(0);
        Background bg = new Background(0);
        VerticalContainer list = new VerticalContainer(8, 4);

        // Title
        Container titleContainer = new Container(4, true, false);
        titleContainer.addWidget(new Label(0, module.title, true));
        list.addWidget(titleContainer);
        list.addWidget(new Separator());

        // Description
        list.addWidget(new Label(0, module.description));
        if (module.settings.size() > 0) list.addWidget(new Separator());

        // Settings
        Grid grid = new Grid(0, 3, 4, 4);
        for (Setting setting : module.settings) {
            Label title = new Label(0, setting.title + ": ");
            title.tooltip = setting.description;

            Widget value;
            if (setting.value() instanceof Boolean) value = new Checkbox(0, (boolean) setting.value(), checkbox -> {
                setting.value(checkbox.checked);
                checkbox.checked = (boolean) setting.value();
            });
            else if (setting.value() instanceof Integer) value = new TextBox(3, Integer.toString((int) setting.value()), 8, TextBoxFilters.integer, textBox -> {
                boolean a = false;
                String val = textBox.text.isEmpty() ? "0" : textBox.text;
                if (val.length() == 1 && val.charAt(0) == '-') {
                    a = true;
                    val = "-0";
                }
                setting.setFromString(val);
                if (!textBox.text.isEmpty() && !a) textBox.text = setting.value().toString();
            });
            else if (setting.value() instanceof Float) value = new TextBox(3, Float.toString((float) setting.value()), 8, TextBoxFilters.floating, textBox -> {
                boolean a = false;
                String val = textBox.text.isEmpty() ? "0" : textBox.text;
                if (val.length() == 1 && val.charAt(0) == '-') {
                    a = true;
                    val = "-0";
                }
                setting.setFromString(val);
                if (!textBox.text.isEmpty() && !a) textBox.text = setting.value().toString();
            });
            else if (setting.value() instanceof Double) value = new TextBox(3, Double.toString((double) setting.value()), 8, TextBoxFilters.floating, textBox -> {
                boolean a = false;
                String val = textBox.text.isEmpty() ? "0" : textBox.text;
                if (val.length() == 1 && val.charAt(0) == '-') {
                    a = true;
                    val = "-0";
                }
                setting.setFromString(val);
                if (!textBox.text.isEmpty() && !a) textBox.text = setting.value().toString();
            });
            else if (setting.value() instanceof Color) value = new ColorEdit(0, 4, (Color) setting.value(), colorEdit -> {
                setting.value(new Color(colorEdit.color));
                colorEdit.setColor((Color) setting.value());
            });
            else if (setting.value() instanceof Enum<?>) value = new EnumButton<>(4, (Enum) setting.value(), enumEnumButton -> {
                setting.value(enumEnumButton.value);
                enumEnumButton.setValue((Enum) setting.value());
            });
            else value = new Label(0, "not-implemented");

            Button reset = new Button(4, "Reset", button -> {
                setting.reset();

                if (value instanceof Checkbox) ((Checkbox) value).checked = (boolean) setting.value();
                else if (value instanceof TextBox) ((TextBox) value).text = setting.value().toString();
                else if (value instanceof ColorEdit) ((ColorEdit) value).setColor((Color) setting.value());
                else if (value instanceof EnumButton) ((EnumButton) value).setValue((Enum) setting.value());
            });

            grid.addRow(title, value, reset);
        }
        list.addWidget(grid);
        list.addWidget(new Separator());

        // Bind
        HorizontalContainer bindContainer = new HorizontalContainer(0, 12);
        bindLabel = bindContainer.addWidget(new Label(0, getBindString()));
        HorizontalContainer bindBtnContainer = new HorizontalContainer(0, 4);
        bindBtnContainer.addWidget(new Button(4, "Set Bind", button -> {
            ModuleManager.moduleToBind = module;
            bindLabel.setText("Bind: press key");
        }));
        bindBtnContainer.addWidget(new Button(4, "Reset Bind", button -> {
            module.setKey(-1);
            bindLabel.setText(getBindString());
        }));
        bindContainer.addWidget(bindBtnContainer);
        list.addWidget(bindContainer);
        list.addWidget(new Separator());

        // Visible
        HorizontalContainer visibleContainer = new HorizontalContainer(0, 4);
        visibleContainer.addWidget(new Label(0, "Visible: "));
        visibleContainer.addWidget(new Checkbox(0, module.isVisible(), checkbox -> {
            if (module.isVisible() != checkbox.checked) module.setVisible(checkbox.checked);
        }));
        list.addWidget(visibleContainer);

        // Active
        HorizontalContainer activeContainer = new HorizontalContainer(0, 4);
        Label activeLabel = activeContainer.addWidget(new Label(0, "Active: "));
        activeLabel.tooltip = "Visible in active modules.";
        activeContainer.addWidget(new Checkbox(0, module.isActive(), checkbox -> {
            if (module.isActive() != checkbox.checked) module.toggle();
        }));
        list.addWidget(activeContainer);

        bg.addWidget(list);
        root.addWidget(bg);
        addWidget(root);

        MeteorClient.eventBus.subscribe(this);
    }

    private String getBindString() {
        return "Bind: " + (module.getKey() == -1 ? "none" : GLFW.glfwGetKeyName(module.getKey(), 0));
    }

    @EventHandler
    private Listener<ModuleBindChangedEvent> onModuleBindChanged = new Listener<>(event -> {
        if (event.module.equals(module)) bindLabel.setText(getBindString());
    });

    @Override
    public void onClose() {
        MeteorClient.eventBus.unsubscribe(this);
        minecraft.openScreen(parent);
    }
}
