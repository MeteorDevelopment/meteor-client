package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.TextBoxFilters;
import minegame159.meteorclient.utils.Utils;

import java.util.function.Consumer;

public class WDoubleTextBox extends WTextBox {
    public double value;
    public Consumer<WDoubleTextBox> action;

    public WDoubleTextBox(double value, int maxCharCount) {
        super(Utils.doubleToString(value), maxCharCount);
        filter = TextBoxFilters.floating;

        this.value = value;

        super.action = this::textChanged;
    }

    private void textChanged(WTextBox textBox) {
        double lastValue = value;
        if (text.isEmpty()) value = 0;
        else if (text.length() == 1 && text.charAt(0) == '-') value = 0;
        else value = Double.parseDouble(text);

        if (action != null && value != lastValue) action.accept(this);
    }

    public void setValue(double value) {
        this.value = value;
        text = Utils.doubleToString(value);
    }
}
