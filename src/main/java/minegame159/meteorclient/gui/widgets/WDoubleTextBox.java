package minegame159.meteorclient.gui.widgets;

import java.util.Locale;

public class WDoubleTextBox extends WTextBox {
    private double value;

    public WDoubleTextBox(double value, double width) {
        super("", width);

        setValue(value);
    }

    @Override
    protected boolean addChar(char c) {
        if (c >= '0' && c <= '9') return true;
        if (c == '-' && getCursor() == 0 && !getText().contains("-")) return true;
        return c == '.' && !getText().contains(".");
    }

    @Override
    protected boolean callActionOnTextChanged() {
        double lastValue = value;

        if (getText().isEmpty()) {
            value = 0;
            return false;
        } else if (getText().equals("-") || getText().equals(".") || getText().equals("-.")) {
            value = -0;
            return false;
        } else {
            try {
                value = Double.parseDouble(getText());
                if (action != null && value != lastValue) {
                    action.run();
                    return true;
                }
                return false;
            } catch (NumberFormatException ignored) {
                setValue(0);
                if (action != null && value != lastValue) action.run();
                return true;
            }
        }
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
        setText(String.format(Locale.US, "%.2f", value));
    }
}
