/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

public class WIntTextBox extends WTextBox {
    private int value;

    public WIntTextBox(int value, double width) {
        super(Integer.toString(value), width);

        this.value = value - 1;
        setValue(value);
    }

    @Override
    protected boolean addChar(char c) {
        if (c >= '0' && c <= '9') return true;
        return c == '-' && getCursor() == 0 && !getText().contains("-");
    }

    @Override
    protected void callActionOnTextChanged() {
        int lastValue = value;

        if (getText().isEmpty() || getText().equals("-")) {
        } else {
            try {
                value = Integer.parseInt(getText());
            } catch (NumberFormatException ignored) {
                try {
                    long longValue = Long.parseLong(getText());
                    value = longValue > Integer.MAX_VALUE ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                } catch (NumberFormatException ignored2) {
                    value = 0;
                }

                setValue(value);
            }
        }

        if (action != null && value != lastValue) action.run();
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        if (this.value != value) {
            this.value = value;
            setText(Integer.toString(value));
        }
    }
}
