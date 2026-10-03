/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;

public class WLongEdit extends WHorizontalList {
    private long value;

    public final long min, max;
    public Runnable action;

    private WTextBox textBox;

    public WLongEdit(long value, long min, long max) {
        this.value = value;
        this.min = min;
        this.max = max;
    }

    @Override
    public void init() {
        textBox = add(theme.textBox(Long.toString(value), (text, c) -> Character.isDigit(c) || c == '-' && !text.contains("-"))).minWidth(75).expandX().widget();

        textBox.actionOnUnfocused = () -> {
            long lastValue = value;

            try {
                long parsed = Long.parseLong(textBox.get().trim());
                if (parsed >= min && parsed <= max) value = parsed;
            } catch (NumberFormatException _) {}

            textBox.set(Long.toString(value));

            if (action != null && value != lastValue) action.run();
        };
    }

    public long get() {
        return value;
    }

    public void set(long value) {
        this.value = value;
        textBox.set(Long.toString(value));
    }
}
