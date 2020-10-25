package minegame159.meteorclient.gui.widgets;

public class WIntTextBox extends WTextBox {
    private int value;

    public WIntTextBox(int value, double width) {
        super(Integer.toString(value), width);

        this.value = value;
    }

    @Override
    protected boolean addChar(char c) {
        if (c >= '0' && c <= '9') return true;
        return c == '-' && getCursor() == 0 && !getText().contains("-");
    }

    @Override
    protected boolean callActionOnTextChanged() {
        int lastValue = value;

        if (getText().isEmpty()) {
            value = 0;
            return false;
        } else if (getText().equals("-")) {
            value = -0;
            return false;
        } else {
            try {
                value = Integer.parseInt(getText());
                if (action != null && value != lastValue) action.run();
                return true;
            } catch (NumberFormatException ignored) {
                try {
                    long longValue = Long.parseLong(getText());
                    value = longValue > Integer.MAX_VALUE ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                } catch (NumberFormatException ignored2) {
                    value = 0;
                }

                setValue(value);
                if (action != null && value != lastValue) action.run();
                return true;
            }
        }
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
        setText(Integer.toString(value));
    }
}
