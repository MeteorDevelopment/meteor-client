/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.widgets;

public class WIntEdit extends WTable {
    public Runnable action;

    private final WIntTextBox textBox;
    private final WSlider slider;

    public WIntEdit(int value, int sliderMin, int sliderMax) {
        textBox = add(new WIntTextBox(value, 60)).getWidget();
        slider = add(new WSlider(value, sliderMin, sliderMax, 200)).fillX().expandX().getWidget();

        textBox.action = () -> {
            if (textBox.getValue() != Math.round(slider.value)) {
                slider.value = textBox.getValue();
                if (action != null) action.run();
            }
        };

        slider.action = wSlider -> {
            if (Math.round(slider.value) != textBox.getValue()) {
                textBox.setValue((int) Math.round(slider.value));
                if (action != null) action.run();
            }
        };
    }

    public int get() {
        return textBox.getValue();
    }

    public void set(int value) {
        textBox.setValue(value);
        if (Math.round(slider.value) != value) slider.value = value;
    }
}
