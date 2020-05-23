package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.listeners.IntEditChangeListener;

public class WIntEdit extends WTable {
    public IntEditChangeListener action;

    private final WIntTextBox textBox;
    private final WSlider slider;

    public WIntEdit(int min, int max, int value) {
        textBox = add(new WIntTextBox(value, 50)).getWidget();
        slider = add(new WSlider(min, max, value, 75)).getWidget();

        textBox.action = textBox1 -> {
            if (textBox1.value != Math.round(slider.value)) {
                slider.value = textBox1.value;
                if (action != null) action.onIntEditChange(this);
            }
        };

        slider.action = slider1 -> {
            if (Math.round(slider1.value) != textBox.value) {
                textBox.setValue((int) Math.round(slider1.value));
                if (action != null) action.onIntEditChange(this);
            }
        };
    }

    public int get() {
        return textBox.value;
    }

    public void set(int value) {
        textBox.setValue(value);
        if (Math.round(slider.value) != value) slider.value = value;
    }
}
