/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

public class WDoubleEdit extends WTable {
    public Runnable action;

    private final WDoubleTextBox textBox;
    private WSlider slider;

    public WDoubleEdit(double value, double sliderMin, double sliderMax, int decimalPlaces, boolean noSlider, double sliderWidth) {
        textBox = add(new WDoubleTextBox(value, 60, decimalPlaces)).getWidget();
        if (!noSlider) slider = add(new WSlider(value, sliderMin, sliderMax, sliderWidth)).fillX().expandX().getWidget();

        textBox.action = () -> {
            if (slider != null) slider.value = textBox.getValue();
            if (action != null) action.run();
        };

        if (slider != null) {
            slider.action = wSlider -> {
                textBox.setValue(slider.value);
                if (action != null) action.run();
            };
        }
    }

    public WDoubleEdit(double value, double sliderMin, double sliderMax) {
        this(value, sliderMin, sliderMax, 2, false, 200);
    }

    public double get() {
        return textBox.getValue();
    }

    public void set(double value) {
        textBox.setValue(value);
        if (slider != null) slider.value = value;
    }
}
