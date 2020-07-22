package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.listeners.DoubleEditChangeListener;

public class WDoubleEdit extends WTable {
    public DoubleEditChangeListener action;

    private final WDoubleTextBox textBox;
    private WSlider slider;

    public WDoubleEdit(double min, double max, double value, boolean noSlider) {
        textBox = add(new WDoubleTextBox(value, 50)).getWidget();
        if (!noSlider) slider = add(new WSlider(min, max, value, 75)).getWidget();

        textBox.action = textBox1 -> {
            if (!noSlider) slider.value = textBox1.value;
            if (action != null) action.onDoubleEditChange(this);
        };

        if (!noSlider) {
            slider.action = slider1 -> {
                textBox.setValue(slider1.value);
                if (action != null) action.onDoubleEditChange(this);
            };
        }
    }

    public WDoubleEdit(double min, double max, double value) {
        this(min, max, value, false);
    }

    public double get() {
        return textBox.value;
    }

    public void set(double value) {
        textBox.setValue(value);
        if (slider != null) slider.value = value;
    }
}
