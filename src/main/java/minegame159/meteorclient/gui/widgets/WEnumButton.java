package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.Vector2;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

public class WEnumButton<T extends Enum<?>> extends WButton {
    public T value;
    public Consumer<WEnumButton<T>> action;

    private T[] values;
    private double width;
    private int valueI;

    public WEnumButton(T value) {
        super(value.toString());
        boundingBox.autoSize = false;

        label.boundingBox.alignment.x = Alignment.X.Center;

        this.value = value;

        try {
            values = (T[]) value.getClass().getMethod("values").invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < values.length; i++) {
            T v = values[i];

            width = Math.max(width, Utils.getTextWidth(v.toString()));
            if (v == value) valueI = i;
        }
    }

    @Override
    public Vector2 calculateCustomSize() {
        return new Vector2(width, Utils.getTextHeight());
    }

    @Override
    public boolean onMousePressed(int button) {
        if (mouseOver) {
            if (button == 0) {
                valueI++;
                updateValue();
                return true;
            } else if (button == 1) {
                valueI--;
                updateValue();
                return true;
            }
        }

        return false;
    }

    private void updateValue() {
        if (valueI < 0) valueI = values.length - 1;
        else if (valueI >= values.length) valueI = 0;

        value = values[valueI];
        label.text = value.toString();

        calculateSize();
        calculatePosition();

        if (action != null) action.accept(this);
    }

    public void setValue(T value) {
        this.value = value;
        label.text = value.toString();

        calculateSize();
        calculatePosition();
    }
}
