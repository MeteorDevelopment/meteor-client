package minegame159.meteorclient.gui.screens;

import me.zero.alpine.event.EventPriority;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.macros.Macro;
import minegame159.meteorclient.macros.MacroManager;
import minegame159.meteorclient.gui.widgets.*;
import org.lwjgl.glfw.GLFW;

public class EditMacroScreen extends WindowScreen implements Listenable {
    private Macro macro;
    private WLabel keyLabel;
    private boolean waitingForKey;

    public EditMacroScreen(Macro m) {
        super("Edit Macro", true);
        this.macro = m == null ? new Macro() : m;

        initWidgets(m);
    }

    private void initWidgets(Macro m) {
        boolean newMacro = m == null;

        // Name
        add(new WLabel("Name:"));
        WTextBox nameT = add(new WTextBox(newMacro ? "" : macro.name, 200)).fillX().expandX().getWidget();
        nameT.setFocused(true);
        nameT.action = textBox -> macro.name = textBox.text.trim();
        row();

        // Messages
        add(new WLabel("Messages:")).padTop(2).top();
        WTable table = add(new WTable()).getWidget();
        fillGridMacroMessages(table);
        row();

        WTextBox newCommand = table.add(new WTextBox("", 200)).fillX().expandX().getWidget();
        WPlus add = table.add(new WPlus()).getWidget();
        add.action = plus -> {
            macro.messages.add(newCommand.text.trim());
            clear();
            initWidgets(macro);
        };

        // Key
        keyLabel = add(new WLabel(getKeyLabelText())).getWidget();
        add(new WButton("Set key")).getWidget().action = button -> {
            waitingForKey = true;
            keyLabel.setText(getKeyLabelText());
        };
        row();

        add(new WHorizontalSeparator());
        row();

        // Apply / Add
        WButton applyAdd = add(new WButton(newMacro ? "Add" : "Apply")).fillX().expandX().getWidget();
        applyAdd.action = button -> {
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
    }

    @Override
    protected void init() {
        super.init();
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private void fillGridMacroMessages(WTable table) {
        for (int i = 0; i < macro.messages.size(); i++) {
            int ii = i;

            WTextBox command = table.add(new WTextBox(macro.messages.get(ii), 200)).getWidget();
            command.action = textBox -> macro.messages.set(ii, textBox.text.trim());

            WMinus remove = new WMinus();
            remove.action = minus -> {
                macro.removeMessage(ii);
                table.clear();
                fillGridMacroMessages(table);
            };

            table.row();
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
            keyLabel.setText(getKeyLabelText());
        }
    }, EventPriority.HIGHEST);

    @Override
    public void onClose() {
        MeteorClient.EVENT_BUS.unsubscribe(this);
        super.onClose();
    }
}
