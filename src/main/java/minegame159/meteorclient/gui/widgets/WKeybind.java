/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.widgets.containers.WHorizontalList;
import minegame159.meteorclient.gui.widgets.pressable.WButton;
import minegame159.meteorclient.utils.misc.Keybind;

public class WKeybind extends WHorizontalList {
    public Runnable action;
    public Runnable actionOnSet;

    private WLabel label;
    private final boolean addBindText;

    private final Keybind keybind;
    private boolean listening;

    public WKeybind(Keybind keybind, boolean addBindText) {
        this.keybind = keybind;
        this.addBindText = addBindText;
    }

    @Override
    public void init() {
        label = add(theme.label("")).widget();

        WButton set = add(theme.button("Set")).widget();
        set.action = () -> {
            listening = true;
            label.set(appendBindText("Press any key or mouse button"));

            if (actionOnSet != null) actionOnSet.run();
        };

        WButton reset = add(theme.button("Reset")).widget();
        reset.action = () -> {
            keybind.set(true, -1);
            reset();

            if (action != null) action.run();
        };

        refreshLabel();
    }

    public boolean onAction(boolean isKey, int value) {
        if (listening && keybind.canBindTo(isKey, value)) {
            keybind.set(isKey, value);
            reset();

            if (action != null) action.run();
            return true;
        }

        return false;
    }

    public void reset() {
        listening = false;
        refreshLabel();
    }

    private void refreshLabel() {
        label.set(appendBindText(keybind.toString()));
    }

    private String appendBindText(String text) {
        return addBindText ? "Bind: " + text : text;
    }
}
