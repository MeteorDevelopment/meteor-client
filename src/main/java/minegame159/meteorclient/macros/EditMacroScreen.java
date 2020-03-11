package minegame159.meteorclient.macros;

import me.zero.alpine.event.EventPriority;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.clickgui.WHorizontalSeparatorBigger;
import minegame159.meteorclient.gui.widgets.*;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class EditMacroScreen extends WidgetScreen implements Listenable {
    private WVerticalList list;
    private Macro macro;
    private WLabel keyLabel;
    private boolean waitingForKey;

    public EditMacroScreen(Macro m) {
        super("Edit Macro");

        boolean newMacro = m == null;
        this.macro = m == null ? new Macro() : m;

        WPanel panel = add(new WPanel());
        panel.boundingBox.setMargin(6);
        panel.boundingBox.alignment.set(Alignment.X.Center, Alignment.Y.Center);

        list = panel.add(new WVerticalList(4));
        list.maxHeight = MinecraftClient.getInstance().window.getScaledHeight() - 32;

        // Title
        list.add(new WLabel(newMacro ? "New Macro" : "Edit Macro", true)).boundingBox.alignment.x = Alignment.X.Center;
        list.add(new WHorizontalSeparatorBigger());

        // Name
        WHorizontalList name = list.add(new WHorizontalList(4));
        name.add(new WLabel("Name:"));
        name.add(new WTextBox(newMacro ? "" : macro.name, 16)).action = textBox -> macro.name = textBox.text.trim();
        list.add(new WHorizontalSeparator());

        // Commands
        list.add(new WLabel("Commands:"));
        WGrid grid = list.add(new WGrid(4, 4, 2));
        for (int i = 0; i < macro.commands.size(); i++) {
            int ii = i;

            WTextBox command = new WTextBox(macro.commands.get(ii), 32);
            command.action = textBox -> macro.commands.set(ii, textBox.text.trim());
            WMinus remove = new WMinus();
            remove.action = () -> {
                macro.removeCommand(ii);
                grid.removeRow(ii);
                layout();
            };

            grid.addRow(command, remove);
        }

        WTextBox newCommand = new WTextBox("", 32);
        WPlus add = new WPlus();
        add.action = () -> {
            grid.removeLastRow();
            macro.commands.add(newCommand.text.trim());

            WTextBox command = new WTextBox(newCommand.text.trim(), 32);
            command.action = textBox -> macro.commands.set(macro.commands.size() - 1, textBox.text.trim());
            WMinus remove = new WMinus();
            remove.action = () -> {
                macro.removeCommand(macro.commands.size() - 1);
                grid.removeRow(macro.commands.size() - 1);
                layout();
            };

            grid.addRow(command, remove);
            newCommand.text = "";
            grid.addRow(newCommand, add);
            layout();
        };

        grid.addRow(newCommand, add);
        list.add(new WHorizontalSeparator());

        // Key
        WHorizontalList keyList = list.add(new WHorizontalList(4));
        keyLabel = keyList.add(new WLabel(getKeyLabelText()));
        keyList.add(new WButton("Set key")).action = () -> {
            waitingForKey = true;
            keyLabel.text = getKeyLabelText();
            layout();
        };
        list.add(new WHorizontalSeparator());

        // Apply / Add
        WButton applyAdd = list.add(new WButton(newMacro ? "Add" : "Apply"));
        applyAdd.boundingBox.alignment.x = Alignment.X.Center;
        applyAdd.action = () -> {
            if (newMacro) {
                if (macro.name != null && !macro.name.isEmpty() && macro.commands.size() > 0 && macro.key != -1) {
                    MacroManager.INSTANCE.add(macro);
                    onClose();
                }
            } else {
                MacroManager.save();
                MeteorClient.eventBus.post(EventStore.macroListChangedEvent());
                onClose();
            }
        };

        layout();
        MeteorClient.eventBus.subscribe(this);
    }

    private String getKeyLabelText() {
        if (waitingForKey) return "Press any key";
        return "Key: " + (macro.key == -1 ? "none" : GLFW.glfwGetKeyName(macro.key, 0));
    }

    @EventHandler
    private Listener<KeyEvent> onKey = new Listener<>(event -> {
        if (waitingForKey) {
            waitingForKey = false;
            macro.key = event.key;
            keyLabel.text = getKeyLabelText();
            layout();
        }
    }, EventPriority.HIGHEST);

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        list.maxHeight = height - 32;
        super.resize(client, width, height);
    }

    @Override
    public void onClose() {
        MeteorClient.eventBus.unsubscribe(this);
        super.onClose();
    }
}
