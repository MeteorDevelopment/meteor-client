package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.GuiRenderer;
import minegame159.meteorclient.gui.listeners.SliderMoveListener;
import minegame159.meteorclient.utils.Utils;

public class WSlider extends WWidget {
    public SliderMoveListener action;

    public double value;

    private double min, max;
    private double uWidth;

    private boolean mouseOverHandle;
    private boolean dragging;
    private double lastMouseX;

    public WSlider(double min, double max, double value, double uWidth) {
        this.min = min;
        this.max = max;
        this.uWidth = uWidth;
        this.value = value;
    }

    @Override
    protected void onCalculateSize() {
        width = uWidth;
        height = 8;
    }

    @Override
    protected boolean onMouseClicked(int button) {
        if (mouseOverHandle) {
            dragging = true;
            return true;
        } else if (mouseOver) {
            double valueWidth = lastMouseX - (x + 4);
            value = (valueWidth / (width - 8)) * (max - min) + min;
            if (action != null) action.onSliderMove(this);

            dragging = true;
            return true;
        }

        return false;
    }

    @Override
    protected boolean onMouseReleased(int button) {
        if (dragging) {
            dragging = false;
        }

        return mouseOver;
    }

    @Override
    protected void onMouseMoved(double mouseX, double mouseY) {
        double valuePercentage = (value - min) / (max - min);
        double valueWidth = valuePercentage * (width - 8);

        double x = this.x + 4 + valueWidth - height / 2;
        mouseOverHandle =  mouseX >= x && mouseX <= x + height && mouseY >= y && mouseY <= y + height;

        boolean mouseOverX = mouseX >= this.x + 4 && mouseX <= this.x + 4 + width - 8;
        mouseOver = mouseOverX && mouseY >= this.y && mouseY <= this.y + height;

        if (dragging) {
            if (mouseOverX) {
                valueWidth += mouseX - lastMouseX;
                valueWidth = Utils.clamp(valueWidth, 0, width - 8);

                value = (valueWidth / (width - 8)) * (max - min) + min;
                if (action != null) action.onSliderMove(this);
            } else {
                if (value > min && mouseX < this.x + 4) {
                    value = min;
                    if (action != null) action.onSliderMove(this);
                } else if (value < max && mouseX > this.x + 4 + width - 8) {
                    value = max;
                    if (action != null) action.onSliderMove(this);
                }
            }
        }

        lastMouseX = mouseX;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        value = Utils.clamp(value, min, max);
        double valuePercentage = (value - min) / (max - min);
        double valueWidth = valuePercentage * (width - 8);

        renderer.renderQuad(x + 4, y + 3, valueWidth, 2, GuiConfig.INSTANCE.sliderLeft);
        renderer.renderQuad(x + 4 + valueWidth, y + 3, width - valueWidth - 8, 2, GuiConfig.INSTANCE.sliderRight);

        renderer.renderQuad(x + 4 + valueWidth - height / 2, y, height, height, GuiRenderer.TEX_SLIDER_HANDLE, GuiRenderer.TEX_SLIDER_HANDLE.getColor(mouseOverHandle, dragging));
    }
}
