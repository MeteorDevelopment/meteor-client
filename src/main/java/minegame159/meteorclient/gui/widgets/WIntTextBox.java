package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.TextBoxFilters;
import minegame159.meteorclient.gui.listeners.IntTextBoxChangeListener;

public class WIntTextBox extends WTextBox {
    public IntTextBoxChangeListener action;

    public int value;

    public WIntTextBox(int value, double width) {
        super(Integer.toString(value), width);
        filter = TextBoxFilters.integer;

        this.value = value;

        super.action = this::textChanged;
    }

    @Override
    protected void callAction() {
        if (text.length() > 1 || (text.length() == 1 && text.charAt(0) != '-')) super.callAction();
    }

    private void textChanged(WTextBox textBox) {
        int lastValue = value;
        if (text.isEmpty()) value = 0;
        else if (text.length() == 1 && text.charAt(0) == '-') value = -0;
        else {
            try {
                value = Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                try {
                    long longValue = Long.parseLong(text);
                    value = longValue > Integer.MAX_VALUE ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                } catch (NumberFormatException ignored2) {
                    value = 0;
                }

                text = Integer.toString(value);
            }
        }

        if (action != null && value != lastValue) action.onIntTextBoxChange(this);
    }

    public void setValue(int value) {
        this.value = value;
        text = Integer.toString(value);
    }
}
