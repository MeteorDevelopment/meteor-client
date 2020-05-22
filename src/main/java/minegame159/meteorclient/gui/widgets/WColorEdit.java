package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.utils.Color;

import java.util.function.Consumer;

public class WColorEdit extends WTable {
    public Color color;
    public Consumer<WColorEdit> action;

    private final WQuad quad;
    private final WIntTextBox r, g, b, a;

    public WColorEdit(Color color) {
        this.color = color;

        quad = add(new WQuad(color)).getWidget();
        r = add(new WIntTextBox(color.r, 20)).getWidget();
        r.action = wIntTextBox -> changed();
        g = add(new WIntTextBox(color.g, 20)).getWidget();
        g.action = wIntTextBox -> changed();
        b = add(new WIntTextBox(color.b, 20)).getWidget();
        b.action = wIntTextBox -> changed();
        a = add(new WIntTextBox(color.a, 20)).getWidget();
        a.action = wIntTextBox -> changed();
    }

    private void changed() {
        color.r = r.value;
        color.g = g.value;
        color.b = b.value;
        color.a = a.value;

        color.validate();
        if (!(r.value == 0 && color.r == 0)) r.setValue(color.r);
        if (!(g.value == 0 && color.g == 0)) g.setValue(color.g);
        if (!(b.value == 0 && color.b == 0)) b.setValue(color.b);
        if (!(a.value == 0 && color.a == 0)) a.setValue(color.a);

        quad.color.set(color);
        if (action != null) action.accept(this);
    }

    public void set(Color color) {
        color.validate();
        this.color.set(color);
        set();
        quad.color.set(color);
    }

    public void set() {
        r.setValue(color.r);
        g.setValue(color.g);
        b.setValue(color.b);
        a.setValue(color.a);
    }
}
