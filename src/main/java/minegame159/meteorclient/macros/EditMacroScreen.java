package minegame159.meteorclient.macros;

import me.zero.alpine.event.EventPriority;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.SaveManager;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.gui.PanelListScreen;
import minegame159.meteorclient.gui.widgets.*;
import org.lwjgl.glfw.GLFW;

public class EditMacroScreen extends PanelListScreen implements Listenable {
    private Macro macro;
    private WLabel keyLabel;
    private boolean waitingForKey;

    public EditMacroScreen(Macro m) {
        super("Edit Macro");

        boolean newMacro = m == null;
        this.macro = m == null ? new Macro() : m;

        // Name
        WHorizontalList name = add(new WHorizontalList(4));
        name.add(new WLabel("Name:"));
        name.add(new WTextBox(newMacro ? "" : macro.name, 16)).action = textBox -> macro.name = textBox.text.trim();
        add(new WHorizontalSeparator());

        // Messages
        add(new WLabel("Messages:"));
        WGrid grid = add(new WGrid(4, 4, 2));
        for (int i = 0; i < macro.messages.size(); i++) {
            int ii = i;

            WTextBox command = new WTextBox(macro.messages.get(ii), 32);
            command.action = textBox -> macro.messages.set(ii, textBox.text.trim());
            WMinus remove = new WMinus();
            remove.action = () -> {
                macro.removeMessage(ii);
                grid.removeRow(ii);
                layout();
            };

            grid.addRow(command, remove);
        }

        WTextBox newCommand = new WTextBox("", 32);
        WPlus add = new WPlus();
        add.action = () -> {
            grid.removeLastRow();
            macro.messages.add(newCommand.text.trim());

            WTextBox command = new WTextBox(newCommand.text.trim(), 32);
            command.action = textBox -> macro.messages.set(macro.messages.size() - 1, textBox.text.trim());
            WMinus remove = new WMinus();
            remove.action = () -> {
                macro.removeMessage(macro.messages.size() - 1);
                grid.removeRow(macro.messages.size() - 1);
                layout();
            };

            grid.addRow(command, remove);
            newCommand.text = "";
            grid.addRow(newCommand, add);
            layout();
        };

        grid.addRow(newCommand, add);
        add(new WHorizontalSeparator());

        // Key
        WHorizontalList keyList = add(new WHorizontalList(4));
        keyLabel = keyList.add(new WLabel(getKeyLabelText()));
        keyList.add(new WButton("Set key")).action = () -> {
            waitingForKey = true;
            keyLabel.text = getKeyLabelText();
            layout();
        };
        add(new WHorizontalSeparator());

        // Apply / Add
        WButton applyAdd = add(new WButton(newMacro ? "Add" : "Apply"));
        applyAdd.boundingBox.alignment.x = Alignment.X.Center;
        applyAdd.action = () -> {
            if (newMacro) {
                if (macro.name != null && !macro.name.isEmpty() && macro.messages.size() > 0 && macro.key != -1) {
                    MacroManager.INSTANCE.add(macro);
                    onClose();
                }
            } else {
                SaveManager.save(MacroManager.class);
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
    public void onClose() {
        MeteorClient.eventBus.unsubscribe(this);
        super.onClose();
    }
}
