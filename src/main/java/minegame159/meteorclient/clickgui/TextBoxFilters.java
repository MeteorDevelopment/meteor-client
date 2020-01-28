package minegame159.meteorclient.clickgui;

import minegame159.meteorclient.clickgui.widgets.TextBoxFilter;

public class TextBoxFilters {
    public static TextBoxFilter integer = (textBox, c) -> {
        if (c >= '0' && c <= '9') return true;
        else return c == '-' && textBox.text.length() == 0;
    };

    public static TextBoxFilter floating = (textBox, c) -> {
        if (c >= '0' && c <= '9') return true;
        else if (c == '-' && textBox.text.length() == 0) return true;
        else return c == '.' && !textBox.text.contains(".") && (textBox.text.charAt(0) != '-' || textBox.text.length() > 1);
    };
}
