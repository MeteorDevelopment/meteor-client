package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.TextBoxFilters;
import minegame159.meteorclient.gui.listeners.DoubleTextBoxChangeListener;
import minegame159.meteorclient.utils.Utils;

public class WDoubleTextBox extends WTextBox {
    public DoubleTextBoxChangeListener action;

    public double value;

    public WDoubleTextBox(double value, double width) {
        super(Utils.doubleToString(value), width);
        filter = TextBoxFilters.floating;

        this.value = value;

        super.action = this::textChanged;
    }

    @Override
    protected void callAction() {
        if (text.length() > 1 || (text.length() == 1 && text.charAt(0) != '-')) super.callAction();
    }

    private void textChanged(WTextBox textBox) {
        double lastValue = value;
        if (text.isEmpty()) value = 0;
        else if (text.length() == 1 && text.charAt(0) == '-') value = 0;
        else {
            try {
                value = Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                value = 0;
                text = Utils.doubleToString(value);
            }
        }

        if (action != null && value != lastValue) action.onDoubleTextBoxChange(this);
    }

    public void setValue(double value) {
        this.value = value;
        text = Utils.doubleToString(value);
    }
}
