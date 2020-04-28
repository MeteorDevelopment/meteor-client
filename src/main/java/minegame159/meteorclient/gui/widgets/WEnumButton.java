package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.GuiRenderer;
import minegame159.meteorclient.gui.listeners.EnumButtonClickListener;
import minegame159.meteorclient.utils.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WEnumButton<T extends Enum<?>> extends WWidget {
    public EnumButtonClickListener<T> action;

    public T value;
    private String valueStr;

    private T[] values;
    private double uWidth, vWidth;
    private int valueI;

    private boolean pressed;

    public WEnumButton(T value) {
        this.value = value;
        this.valueStr = value.toString();
        this.vWidth = Utils.getTextWidth(value.toString());

        try {
            Method method = value.getClass().getMethod("values");
            boolean isAccessible = method.isAccessible();
            method.setAccessible(true);
            values = (T[]) method.invoke(null);
            method.setAccessible(isAccessible);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        for (T v : values) {
            uWidth = Math.max(uWidth, Utils.getTextWidth(v.toString()));
        }

        findValueI();
    }

    private void findValueI() {
        for (int i = 0; i < values.length; i++) {
            T v = values[i];

            if (v == value) valueI = i;
        }
    }

    @Override
    protected void onCalculateSize() {
        width = 3 + uWidth + 3;
        height = 3 + Utils.getTextHeight() + 3;
    }

    private void updateValue() {
        if (valueI < 0) valueI = values.length - 1;
        else if (valueI >= values.length) valueI = 0;

        value = values[valueI];
        valueStr = value.toString();
        vWidth = Utils.getTextWidth(value.toString());

        if (action != null) action.onEnumButtonClick(this);

        invalidate();
    }

    public void setValue(T value) {
        this.value = value;
        valueStr = value.toString();
        vWidth = Utils.getTextWidth(value.toString());

        findValueI();
        invalidate();
    }

    @Override
    protected boolean onMouseClicked(int button) {
        if (mouseOver) {
            pressed = true;
            return true;
        }

        return false;
    }

    @Override
    protected boolean onMouseReleased(int button) {
        if (mouseOver) {
            pressed = false;

            if (button == 0) {
                valueI++;
                updateValue();
                callAction();
            } else if (button == 1) {
                valueI--;
                updateValue();
                callAction();
            }

            return true;
        }

        return false;
    }

    private void callAction() {
        if (action != null) action.onEnumButtonClick(this);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.renderBackground(this, mouseOver, pressed);
        renderer.renderText(valueStr, x + 3 + uWidth / 2 - vWidth / 2, y + 3.5, GuiConfig.INSTANCE.text, false);
    }
}
