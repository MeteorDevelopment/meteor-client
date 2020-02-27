package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.TextBoxFilters;

import java.util.function.Consumer;

public class WIntTextBox extends WTextBox {
    public int value;
    public Consumer<WIntTextBox> action;

    public WIntTextBox(int value, int maxCharCount) {
        super(Integer.toString(value), maxCharCount);
        filter = TextBoxFilters.integer;

        this.value = value;

        super.action = this::textChanged;
    }

    private void textChanged(WTextBox textBox) {
        int lastValue = value;
        if (text.isEmpty()) value = 0;
        else if (text.length() == 1 && text.charAt(0) == '-') value = 0;
        else value = Integer.parseInt(text);

        if (action != null && value != lastValue) action.accept(this);
    }

    public void setValue(int value) {
        this.value = value;
        text = Integer.toString(value);
    }
}
