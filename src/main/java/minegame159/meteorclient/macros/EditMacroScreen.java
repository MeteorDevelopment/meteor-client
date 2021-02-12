/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.macros;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.events.meteor.KeyEvent;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.utils.Utils;

public class EditMacroScreen extends WindowScreen {
    private final Macro macro;
    private final boolean isNewMacro;

    private WLabel keyLabel;
    private boolean waitingForKey;

    public EditMacroScreen(Macro m) {
        super(m == null ? "Create Macro" : "Edit Macro", true);
        isNewMacro = m == null;
        this.macro = isNewMacro ? new Macro() : m;

        initWidgets(m);
    }

    private void initWidgets(Macro m) {
        // Name
        add(new WLabel("Name:"));
        WTextBox name = add(new WTextBox(m == null ? "" : macro.name, 400)).fillX().expandX().getWidget();
        name.setFocused(true);
        name.action = () -> macro.name = name.getText().trim();
        row();

        // Messages
        add(new WLabel("Messages:")).padTop(4).top();
        WTable lines = add(new WTable()).getWidget();
        fillMessagesTable(lines);
        row();

        // Key
        keyLabel = add(new WLabel(getKeyLabelText())).getWidget();
        add(new WButton("Set key")).getWidget().action = () -> {
            waitingForKey = true;
            keyLabel.setText(getKeyLabelText());
        };
        row();

        // Apply
        WButton apply = add(new WButton(isNewMacro ? "Add" : "Apply")).fillX().expandX().getWidget();
        apply.action = () -> {
            if (isNewMacro) {
                if (macro.name != null && !macro.name.isEmpty() && macro.messages.size() > 0 && macro.key != -1) {
                    Macros.get().add(macro);
                    onClose();
                }
            } else {
                Macros.get().save();
                onClose();
            }
        };
    }

    private void fillMessagesTable(WTable lines) {
        if (macro.messages.isEmpty())
            macro.addMessage("");

        for (int i = 0; i < macro.messages.size(); i++) {
            int ii = i;

            WTextBox line = lines.add(new WTextBox(macro.messages.get(i), 400)).getWidget();
            line.action = () -> macro.messages.set(ii, line.getText().trim());

            if (i != macro.messages.size() - 1) {
                WMinus remove = lines.add(new WMinus()).getWidget();
                remove.action = () -> {
                    macro.removeMessage(ii);
                    clear();
                    initWidgets(macro);
                };
            } else {
                WPlus add = lines.add(new WPlus()).getWidget();
                add.action = () -> {
                    macro.addMessage("");
                    clear();
                    initWidgets(macro);
                };
            }

            lines.row();
        }
    }

    private String getKeyLabelText() {
        if (waitingForKey) return "Press any key";
        return "Key: " + (macro.key == -1 ? "none" : Utils.getKeyName(macro.key));
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.HIGHEST + 2)
    private void onKey(KeyEvent event) {
        if (waitingForKey) {
            waitingForKey = false;
            macro.key = event.key;
            keyLabel.setText(getKeyLabelText());
        }
    }
}
