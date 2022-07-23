/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;

import java.util.Locale;

public class WDoubleEdit extends WHorizontalList {
    private double value;

    private final double min, max;
    private final double sliderMin, sliderMax;

    public int decimalPlaces = 3;
    public boolean noSlider = false;
    public boolean small = false;

    public Runnable action;
    public Runnable actionOnRelease;

    private WTextBox textBox;
    private WSlider slider;

    public WDoubleEdit(double value, double min, double max, double sliderMin, double sliderMax, int decimalPlaces, boolean noSlider) {
        this.value = value;
        this.min = min;
        this.max = max;
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;

        if (noSlider || (sliderMin == 0 && sliderMax == 0)) this.noSlider = true;
     }

    @Override
    public void init() {
        textBox = add(theme.textBox(valueString(), this::filter)).minWidth(75).widget();

        if (noSlider) {
            add(theme.button("+")).widget().action = () -> setButton(get() + 1);
            add(theme.button("-")).widget().action = () -> setButton(get() - 1);
        }
        else slider = add(theme.slider(value, sliderMin, sliderMax)).minWidth(small ? 200 - 75 - spacing : 200).centerY().expandX().widget();

        textBox.actionOnUnfocused = () -> {
            double lastValue = value;

            if (textBox.get().isEmpty()) value = 0;
            else if (textBox.get().equals("-")) value = -0;
            else if (textBox.get().equals(".")) value = 0;
            else if (textBox.get().equals("-.")) value = 0;
            else value = Double.parseDouble(textBox.get());

            double preValidationValue = value;

            if (value < min) value = min;
            else if (value > max) value = max;

            if (value != preValidationValue) textBox.set(valueString());
            if (slider != null) slider.set(value);

            if (value != lastValue) {
                if (action != null) action.run();
                if (actionOnRelease != null) actionOnRelease.run();
            }
        };

        if (slider != null) {
            slider.action = () -> {
                double lastValue = value;

                value = slider.get();
                textBox.set(valueString());

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
        else if (c == '.' && !text.contains(".")) {
            good = true;
            if (text.isEmpty()) validate = false;
        }
        else good = Character.isDigit(c);

        if (good && validate) {
            try {
                Double.parseDouble(text + c);
            } catch (NumberFormatException ignored) {
                good = false;
            }
        }

        return good;
    }

    private void setButton(double v) {
        if (this.value == v) return;

        if (v < min) this.value = min;
        else if (v > max) this.value = max;
        else this.value = v;

        if (this.value == v) {
            textBox.set(valueString());
            if (slider != null) slider.set(this.value);

            if (action != null) action.run();
            if (actionOnRelease != null) actionOnRelease.run();
        }
    }

    public double get() {
        return value;
    }

    public void set(double value) {
		this.value = value;

        textBox.set(valueString());
        if (slider != null) slider.set(value);
    }

    private String valueString() {
        return String.format(Locale.US, "%." + decimalPlaces + "f", value);
    }
}
