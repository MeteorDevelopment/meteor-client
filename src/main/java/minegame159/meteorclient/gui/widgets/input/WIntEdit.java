/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets.input;

import minegame159.meteorclient.gui.widgets.containers.WHorizontalList;

public class WIntEdit extends WHorizontalList {
    public Runnable action;
    public Runnable actionOnRelease;

    public boolean hasSlider = true;
    public boolean small;

    private int value;

    private final int sliderMin, sliderMax;
    public Integer min, max;

    private WTextBox textBox;
    private WSlider slider;

    public WIntEdit(int value, int sliderMin, int sliderMax) {
        this.value = value;
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;
    }

    @Override
    public void init() {
        textBox = add(theme.textBox(Integer.toString(value), this::filter)).minWidth(75).widget();
        if (hasSlider) slider = add(theme.slider(value, sliderMin, sliderMax)).minWidth(small ? 200 - 75 - spacing : 200).centerY().expandX().widget();

        textBox.actionOnUnfocused = () -> {
            int lastValue = value;

            if (textBox.get().isEmpty()) value = 0;
            else if (textBox.get().equals("-")) value = -0;
            else value = Integer.parseInt(textBox.get());

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

        if (c == '-' && text.isEmpty()) {
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

    public int get() {
        return value;
    }

    public void set(int value) {
        this.value = value;

        textBox.set(Integer.toString(value));
        if (slider != null) slider.set(value);
    }
}
