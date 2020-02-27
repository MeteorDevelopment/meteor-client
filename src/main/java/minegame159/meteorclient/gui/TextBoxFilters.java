package minegame159.meteorclient.gui;

import minegame159.meteorclient.gui.widgets.WTextBox;

public class TextBoxFilters {
    public static WTextBox.Filter integer = (textBox, c) -> {
        if (c >= '0' && c <= '9') return true;
        return c == '-' && textBox.text.length() == 0;
    };

    public static WTextBox.Filter floating = (textBox, c) -> {
        if (c >= '0' && c <= '9') return true;
        if (c == '-' && textBox.text.length() == 0) return true;
        return c == '.' && textBox.text.length() > 0 && !textBox.text.contains(".") && (textBox.text.charAt(0) != '-' || textBox.text.length() > 1);
    };
}
