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
    public Runnable actionOnRelease;

    private WTextBox textBox;

    public WLongEdit(long value, long min, long max) {
        this.value = value;
        this.min = min;
        this.max = max;
    }

    @Override
    public void init() {
        textBox = add(theme.textBox(Long.toString(value), this::filter)).minWidth(75).expandX().widget();

        textBox.actionOnUnfocused = () -> {
            long lastValue = value;

            if (textBox.get().isEmpty() || textBox.get().equals("-")) {
                value = Math.clamp(0L, min, max);
            } else {
                try {
                    long parsed = Long.parseLong(textBox.get().trim());
                    value = Math.clamp(parsed, min, max);
                } catch (NumberFormatException _) {
                    // Retain current value if input cannot be parsed as a long
                }
            }

            textBox.set(Long.toString(value));

            if (value != lastValue) {
                if (action != null) action.run();
                if (actionOnRelease != null) actionOnRelease.run();
            }
        };
    }

    private boolean filter(String text, char c) {
        boolean good;
        boolean validate = true;

        if (c == '-' && !text.contains("-") && textBox.cursor == 0) {
            good = true;
            validate = false;
        } else {
            good = Character.isDigit(c);
        }

        if (good && validate) {
            try {
                Long.parseLong(text + c);
            } catch (NumberFormatException _) {
                good = false;
            }
        }

        return good;
    }

    public long get() {
        return value;
    }

    public void set(long value) {
        this.value = value;
        textBox.set(Long.toString(this.value));
    }
}
