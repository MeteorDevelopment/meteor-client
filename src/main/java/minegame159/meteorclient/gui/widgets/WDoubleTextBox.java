/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import java.util.Locale;

public class WDoubleTextBox extends WTextBox {
    private double value;

    public WDoubleTextBox(double value, double width) {
        super("", width);

        setValue(value);
    }

    @Override
    protected boolean addChar(char c) {
        if (c >= '0' && c <= '9') return true;
        if (c == '-' && getCursor() == 0 && !getText().contains("-")) return true;
        return c == '.' && !getText().contains(".");
    }

    @Override
    protected void callActionOnTextChanged() {
        double lastValue = value;

        if (getText().isEmpty() || getText().equals("-") || getText().equals(".") || getText().equals("-.")) {
        } else {
            try {
                value = Double.parseDouble(getText());
            } catch (NumberFormatException ignored) {
                setValue(0);
            }
        }

        if (action != null && value != lastValue) action.run();
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        if (this.value != value) {
            this.value = value;
            setText(String.format(Locale.US, "%.2f", value));
        }
    }
}
