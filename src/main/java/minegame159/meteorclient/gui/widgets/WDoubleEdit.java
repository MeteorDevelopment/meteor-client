package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.listeners.DoubleEditChangeListener;

public class WDoubleEdit extends WTable {
    public DoubleEditChangeListener action;

    private final WDoubleTextBox textBox;
    private final WSlider slider;

    public WDoubleEdit(double min, double max, double value) {
        textBox = add(new WDoubleTextBox(value, 50)).getWidget();
        slider = add(new WSlider(min, max, value, 75)).getWidget();

        textBox.action = textBox1 -> {
            slider.value = textBox1.value;
            if (action != null) action.onDoubleEditChange(this);
        };

        slider.action = slider1 -> {
            textBox.setValue(slider1.value);
            if (action != null) action.onDoubleEditChange(this);
        };
    }

    public double get() {
        return textBox.value;
    }

    public void set(double value) {
        textBox.setValue(value);
        slider.value = value;
    }
}
