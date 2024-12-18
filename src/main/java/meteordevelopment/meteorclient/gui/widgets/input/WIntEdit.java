/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;

public class WIntEdit extends WHorizontalList {
    private int value;

    public final int min, max;
    private final int sliderMin, sliderMax;
    public boolean noSlider = false;
    public boolean small = false;

    public Runnable action;
    public Runnable actionOnRelease;

    private WTextBox textBox;
    private WSlider slider;

    public WIntEdit(int value, int min, int max, int sliderMin, int sliderMax, boolean noSlider) {
        this.value = value;
        this.min = min;
        this.max = max;
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;

        if (noSlider || (sliderMin == 0 && sliderMax == 0)) this.noSlider = true;
    }

    @Override
    public void init() {
        textBox = add(theme.textBox(Integer.toString(value), this::filter)).minWidth(75).widget();

        if (noSlider) {
            add(theme.button("+")).widget().action = () -> setButton(get() + 1);
            add(theme.button("-")).widget().action = () -> setButton(get() - 1);
        }
        else {
            slider = add(theme.slider(value, sliderMin, sliderMax)).minWidth(small ? 200 - 75 - spacing : 200).centerY().expandX().widget();
        }

        textBox.actionOnUnfocused = () -> {
            int lastValue = value;

            if (textBox.get().isEmpty()) value = 0;
            else if (textBox.get().equals("-")) value = -0;
            else {
                try {
                    value = Integer.parseInt(textBox.get());
                } catch (NumberFormatException ignored) {}
            }

            if (slider != null) slider.set(value);

            if (value != lastValue) {
                if (action != null) action.run();
                if (actionOnRelease != null) actionOnRelease.run();
            }
        };

        if (slider != null) {
            slider.action = () -> {
                int lastValue = value;

                value = (int) Math.round(slider.get());
                textBox.set(Integer.toString(value));

                if (action != null && value != lastValue) action.run();
            };

            slider.actionOnRelease = () -> {
                if (actionOnRelease != null) actionOnRelease.run();
            };
        }
    }

    private boolean filter(String text, char c) {
        boolean good;
        boolean validate = true;

        if (c == '-' && !text.contains("-") && textBox.cursor == 0) {
            good = true;
            validate = false;
        }
        else good = Character.isDigit(c);

        if (good && validate) {
            try {
                Integer.parseInt(text + c);
            } catch (NumberFormatException ignored) {
                good = false;
            }
        }

        return good;
    }

    private void setButton(int v) {
        if (this.value == v) return;

        if (v < min) this.value = min;
        else this.value = Math.min(v, max);

        if (this.value == v) {
            textBox.set(Integer.toString(this.value));
            if (slider != null) slider.set(this.value);

            if (action != null) action.run();
            if (actionOnRelease != null) actionOnRelease.run();
        }
    }

    public int get() {
        return value;
    }

    public void set(int value) {
		this.value = value;

        textBox.set(Integer.toString(value));
        if (slider != null) slider.set(value);
    }
}
