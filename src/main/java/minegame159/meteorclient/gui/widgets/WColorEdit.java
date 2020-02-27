package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.utils.Color;

import java.util.function.Consumer;

public class WColorEdit extends WHorizontalList {
    public Color color;
    public Consumer<WColorEdit> action;

    private WQuad quad;
    private WIntTextBox r, g, b, a;

    public WColorEdit(Color color) {
        super(4);

        this.color = color;

        quad = add(new WQuad(color));
        r = add(new WIntTextBox(color.r, 3));
        r.action = wIntTextBox -> changed();
        g = add(new WIntTextBox(color.g, 3));
        g.action = wIntTextBox -> changed();
        b = add(new WIntTextBox(color.b, 3));
        b.action = wIntTextBox -> changed();
        a = add(new WIntTextBox(color.a, 3));
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
        this.color = color;
        set();
    }

    public void set() {
        r.setValue(color.r);
        g.setValue(color.g);
        b.setValue(color.b);
        a.setValue(color.a);
    }
}
