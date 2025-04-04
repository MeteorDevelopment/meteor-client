/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.widgets;

import motordevelopment.motorclient.gui.widgets.containers.WHorizontalList;
import motordevelopment.motorclient.gui.widgets.pressable.WButton;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.utils.misc.Keybind;

public class WKeybind extends WHorizontalList {
    public Runnable action;
    public Runnable actionOnSet;

    private WButton button;

    private final Keybind keybind;
    private final Keybind defaultValue;
    private boolean listening;

    public WKeybind(Keybind keybind, Keybind defaultValue) {
        this.keybind = keybind;
        this.defaultValue = defaultValue;
    }

    @Override
    public void init() {
        button = add(theme.button("")).widget();
        button.action = () -> {
            listening = true;
            button.set("...");

            if (actionOnSet != null) actionOnSet.run();
        };

        refreshLabel();
    }

    public boolean onClear() {
        if (listening) {
            keybind.reset();
            reset();

            return true;
        }

        return false;
    }

    public boolean onAction(boolean isKey, int value, int modifiers) {
        if (listening && keybind.canBindTo(isKey, value, modifiers)) {
            keybind.set(isKey, value, modifiers);
            reset();

            return true;
        }

        return false;
    }

    public void resetBind() {
        keybind.set(defaultValue);
        reset();
    }

    public void reset() {
        listening = false;
        refreshLabel();
        if (Modules.get().isBinding()) {
            Modules.get().setModuleToBind(null);
        }
    }

    private void refreshLabel() {
        button.set(keybind.toString());
    }
}
