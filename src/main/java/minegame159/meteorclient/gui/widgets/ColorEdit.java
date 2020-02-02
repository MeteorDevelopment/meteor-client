package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.TextBoxFilters;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;

import java.util.function.Consumer;

public class ColorEdit extends HorizontalContainer {
    public Color color;
    private Quad colorDisplay;
    private TextBox r, g, b, a;

    public ColorEdit(double margin, double spacing, Color color, Consumer<ColorEdit> action) {
        super(margin, spacing);
        this.color = color;

        r = new TextBox(3, Integer.toString(color.r), 3, TextBoxFilters.integer, textBox -> {
            color.r = textBox.text.isEmpty() ? 0 : Integer.parseInt(textBox.text);
            color.validate();
            action.accept(this);
        });
        g = new TextBox(3, Integer.toString(color.g), 3, TextBoxFilters.integer, textBox -> {
            color.g = textBox.text.isEmpty() ? 0 : Integer.parseInt(textBox.text);
            color.validate();
            action.accept(this);
        });
        b = new TextBox(3, Integer.toString(color.b), 3, TextBoxFilters.integer, textBox -> {
            color.b = textBox.text.isEmpty() ? 0 : Integer.parseInt(textBox.text);
            color.validate();
            action.accept(this);
        });
        a = new TextBox(3, Integer.toString(color.a), 3, TextBoxFilters.integer, textBox -> {
            color.a = textBox.text.isEmpty() ? 0 : Integer.parseInt(textBox.text);
            color.validate();
            action.accept(this);
        });

        colorDisplay = addWidget(new Quad(Utils.getTextHeight() + 3 + 3, Utils.getTextHeight() + 3 + 3, 0, color));
        addWidgets(r, g, b, a);
    }

    public void setColor(Color color) {
        this.color.set(color);

        r.text = Integer.toString(color.r);
        g.text = Integer.toString(color.g);
        b.text = Integer.toString(color.b);
        a.text = Integer.toString(color.a);

        colorDisplay.color.set(color);
    }
}
