package minegame159.meteorclient.macros;

import me.zero.alpine.event.EventPriority;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.*;
import org.lwjgl.glfw.GLFW;

public class EditMacroScreen extends WindowScreen implements Listenable {
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
        WTextBox nameT = name.add(new WTextBox(newMacro ? "" : macro.name, 200));
        nameT.setFocused(true);
        nameT.action = textBox -> macro.name = textBox.text.trim();
        add(new WHorizontalSeparator());

        // Messages
        add(new WLabel("Messages:"));
        WGrid grid = add(new WGrid(4, 4, 2));
        fillGridMacroMessages(grid);

        WTextBox newCommand = new WTextBox("", 200);
        WPlus add = new WPlus();
        add.action = () -> {
            grid.removeLastRow();
            macro.messages.add(newCommand.text.trim());

            WTextBox command = new WTextBox(newCommand.text.trim(), 200);
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
                MacroManager.INSTANCE.save();
                MeteorClient.EVENT_BUS.post(EventStore.macroListChangedEvent());
                onClose();
            }
        };

        layout();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private void fillGridMacroMessages(WGrid grid) {
        for (int i = 0; i < macro.messages.size(); i++) {
            int ii = i;

            WTextBox command = new WTextBox(macro.messages.get(ii), 200);
            command.action = textBox -> macro.messages.set(ii, textBox.text.trim());
            WMinus remove = new WMinus();
            remove.action = () -> {
                macro.removeMessage(ii);
                grid.clear();
                fillGridMacroMessages(grid);
                layout();
            };

            grid.addRow(command, remove);
        }
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
        MeteorClient.EVENT_BUS.unsubscribe(this);
        super.onClose();
    }
}
