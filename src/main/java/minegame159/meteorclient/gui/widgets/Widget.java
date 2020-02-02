package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.RenderUtils;
import net.minecraft.client.font.TextRenderer;

import java.util.ArrayList;
import java.util.List;

public abstract class Widget {
    public double x, y;
    public double width, height;
    public double margin;

    public String tooltip;
    private boolean mouseOver;
    private int tooltipTimer = 20;

    protected Widget parent;
    protected List<Widget> widgets = new ArrayList<>();

    private double maxX;
    private double maxY;

    public Widget(double width, double height, double margin) {
        this.width = width;
        this.height = height;
        this.margin = margin;
    }

    public <T extends Widget> T addWidget(T widget) {
        widget.parent = this;
        widgets.add(widget);
        layout();
        return widget;
    }

    public void addWidgets(Widget... widgets) {
        for (Widget widget : widgets) {
            widget.parent = this;
            this.widgets.add(widget);
        }

        layout();
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + widthMargin() && mouseY >= y && mouseY <= y + heightMargin();
    }

    public void onWindowResized(double windowWidth, double windowHeight) {
        for (Widget widget : widgets) widget.onWindowResized(windowWidth, windowHeight);
    }

    public void onMouseMoved(double mouseX, double mouseY) {
        boolean preMouseOver = mouseOver;
        mouseOver = isMouseOver(mouseX, mouseY);

        if (!preMouseOver && mouseOver) tooltipTimer = 20;

        for (Widget widget : widgets) widget.onMouseMoved(mouseX, mouseY);
    }

    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        for (Widget widget : widgets) {
            if (widget.onMouseClicked(mouseX, mouseY, button)) return true;
        }

        return false;
    }

    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        for (Widget widget : widgets) {
            if (widget.onMouseReleased(mouseX, mouseY, button)) return true;
        }

        return false;
    }

    public boolean onKeyPressed(int key) {
        for (Widget widget : widgets) {
            if (widget.onKeyPressed(key)) return true;
        }

        return false;
    }

    public boolean onCharTyped(char c) {
        for (Widget widget : widgets) {
            if (widget.onCharTyped(c)) return true;
        }

        return false;
    }

    protected void parentLayout() {
        if (parent != null) parent.layout();
    }

    public void layout() {
        for (Widget widget : widgets) {
            widget.x = x + margin;
            widget.y = y + margin;

            widget.layout();
        }
    }

    public void tick() {
        tooltipTimer--;

        for (Widget widget : widgets) widget.tick();
    }

    public void render(double mouseX, double mouseY) {
        for (Widget widget : widgets) widget.render(mouseX, mouseY);
    }

    public void renderText(double mouseX, double mouseY, TextRenderer font) {
        if (mouseOver && tooltipTimer <= 0 && tooltip != null) {
            font.drawWithShadow(tooltip, (float) (mouseX + 8), (float) (mouseY + 8), GUI.textC);
        }

        for (Widget widget : widgets) widget.renderText(mouseX, mouseY, font);
    }

    protected void calculateSize() {
        maxX = x;
        maxY = y;

        for (Widget widget : widgets) maxSize(widget);

        width = maxX - margin - x;
        height = maxY - margin - y;
    }

    private void maxSize(Widget widget) {
        maxX = Math.max(maxX, widget.x + widget.widthMargin());
        maxY = Math.max(maxY, widget.y + widget.heightMargin());

        for (Widget w : widget.widgets) maxSize(w);
    }

    public double widthMargin() {
        return width + margin * 2;
    }

    public double heightMargin() {
        return height + margin * 2;
    }

    // Render things

    protected void renderBackgroundWithOutline(Color backgroundColor, Color outlineColor) {
        quad(x, y, x + widthMargin(), y + heightMargin(), backgroundColor); // Background

        quad(x, y, x + widthMargin(), y + 1, outlineColor); // Top
        quad(x, y + heightMargin(), x + widthMargin(), y + heightMargin() - 1, outlineColor); // Bottom
        quad(x, y, x + 1, y + heightMargin(), outlineColor); // Left
        quad(x + widthMargin(), y, x + widthMargin() - 1, y + heightMargin(), outlineColor); // Left
    }

    protected void quad(double x1, double y1, double x2, double y2, Color color) {
        RenderUtils.quad(x1, y1, 0, x2, y1, 0, x2, y2, 0, x1, y2, 0, color);
    }
}
