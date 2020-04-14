package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.TextBoxFilters;

import java.util.function.Consumer;

public class WIntTextBox extends WTextBox {
    public int value;
    public Consumer<WIntTextBox> action;

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
        else if (text.length() == 1 && text.charAt(0) == '-') value =-0;
        else value = Integer.parseInt(text);

        if (action != null && value != lastValue) action.accept(this);
    }

    public void setValue(int value) {
        this.value = value;
        text = Integer.toString(value);
    }
}
