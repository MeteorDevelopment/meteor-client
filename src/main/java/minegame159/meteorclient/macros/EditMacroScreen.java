/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.macros;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.events.meteor.KeyEvent;
import minegame159.meteorclient.events.meteor.MouseButtonEvent;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.*;

public class EditMacroScreen extends WindowScreen {
    private final Macro macro;
    private final boolean isNewMacro;

    private WKeybind keybind;
    private boolean binding;

    public EditMacroScreen(Macro m) {
        super(m == null ? "Create Macro" : "Edit Macro", true);
        isNewMacro = m == null;
        this.macro = isNewMacro ? new Macro() : m;

        initWidgets(m);
    }

    private void initWidgets(Macro m) {
        // Name
        WTable t = add(new WTable()).getWidget();

        t.add(new WLabel("Name:"));
        WTextBox name = t.add(new WTextBox(m == null ? "" : macro.name, 400)).fillX().expandX().getWidget();
        name.setFocused(true);
        name.action = () -> macro.name = name.getText().trim();
        t.row();

        // Messages
        t.add(new WLabel("Messages:")).padTop(4).top();
        WTable lines = t.add(new WTable()).getWidget();
        fillMessagesTable(lines);
        row();

        // Key
        keybind = add(new WKeybind(macro.keybind)).getWidget();
        keybind.actionOnSet = () -> binding = true;
        row();

        // Apply
        WButton apply = add(new WButton(isNewMacro ? "Add" : "Apply")).fillX().expandX().getWidget();
        apply.action = () -> {
            if (isNewMacro) {
                if (macro.name != null && !macro.name.isEmpty() && macro.messages.size() > 0 && macro.keybind.isSet()) {
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

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onKey(KeyEvent event) {
        if (onAction(true, event.key)) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onButton(MouseButtonEvent event) {
        if (onAction(false, event.button)) event.cancel();
    }

    private boolean onAction(boolean isKey, int value) {
        if (binding) {
            keybind.onAction(isKey, value);

            binding = false;
            return true;
        }

        return false;
    }
}
