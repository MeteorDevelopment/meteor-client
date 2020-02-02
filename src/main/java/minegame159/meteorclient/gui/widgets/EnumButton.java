package minegame159.meteorclient.gui.widgets;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

public class EnumButton<T extends Enum> extends Button {
    public T value;
    private Consumer<EnumButton<T>> action;
    private T[] values;
    private int valueI;

    public EnumButton(double margin, T value, Consumer<EnumButton<T>> action) {
        super(margin, value.toString(), null);
        this.action = action;

        try {
            values = (T[]) value.getClass().getMethod("values").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                valueI = i;
                break;
            }
        }
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0) {
            valueI++;
            if (valueI >= values.length) valueI = 0;
            value = values[valueI];
            setText(value.toString());
            if (action != null) action.accept(this);
            return true;
        } else if (isMouseOver(mouseX, mouseY) && button == 1) {
            valueI--;
            if (valueI < 0) valueI = values.length - 1;
            value = values[valueI];
            setText(value.toString());
            if (action != null) action.accept(this);
            return true;
        }

        return false;
    }

    public void setValue(T value) {
        this.value = value;
        setText(value.toString());
    }
}
