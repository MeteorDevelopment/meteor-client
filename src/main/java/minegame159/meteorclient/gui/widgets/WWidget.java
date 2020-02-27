package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.BoundingBox;
import minegame159.meteorclient.gui.WidgetLayout;
import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.*;

import java.util.ArrayList;
import java.util.List;

public class WWidget {
    public BoundingBox boundingBox = new BoundingBox(this::calculateCustomSize);
    public boolean visible = true;
    public String tooltip;

    public boolean mouseOver;

    private int tooltipTimer = 0;

    public WWidget parent;
    public List<WWidget> widgets = new ArrayList<>(2);
    public WidgetLayout layout = new DefaultLayout();

    public <T extends WWidget> T add(T widget) {
        widget.parent = this;
        widgets.add(widget);
        return widget;
    }

    public void mouseMove(double mouseX, double mouseY) {
        boolean lastMoveOver = mouseOver;
        mouseOver = boundingBox.isOver(mouseX, mouseY);
        if (!lastMoveOver && mouseOver) tooltipTimer = 0;

        onMouseMove(mouseX, mouseY);
        for (WWidget widget : widgets) widget.mouseMove(mouseX, mouseY);
    }
    public void onMouseMove(double mouseX, double mouseY) {}

    public boolean mousePressed(int button) {
        for (WWidget widget : widgets) {
            if (widget.mousePressed(button)) return true;
        }
        return onMousePressed(button);
    }
    public boolean onMousePressed(int button) {
        return false;
    }

    public boolean mouseReleased(int button) {
        for (WWidget widget : widgets) {
            if (widget.mouseReleased(button)) return true;
        }
        return onMouseReleased(button);
    }
    public boolean onMouseReleased(int button) {
        return false;
    }

    public boolean mouseScrolled(double amount) {
        for (WWidget widget : widgets) {
            if (widget.mouseScrolled(amount)) return true;
        }
        return onMouseScrolled(amount);
    }
    public boolean onMouseScrolled(double amount) {
        return false;
    }

    public boolean keyPressed(int key) {
        for (WWidget widget : widgets) {
            if (widget.keyPressed(key)) return true;
        }
        return onKeyPressed(key);
    }
    public boolean onKeyPressed(int key) {
        return false;
    }

    public boolean charTyped(char c) {
        for (WWidget widget : widgets) {
            if (widget.charTyped(c)) return true;
        }
        return onCharTyped(c);
    }
    public boolean onCharTyped(char c) {
        return false;
    }

    public void windowResized(int width, int height) {
        for (WWidget widget : widgets) widget.windowResized(width, height);
        onWindowResized(width, height);
    }
    public void onWindowResized(int width, int height) {}

    public void calculateSize() {
        for (WWidget widget : widgets) {
            widget.calculateSize();

            if (!widget.boundingBox.autoSize) widget.boundingBox.calculateCustomSize();
            else if (!widget.boundingBox.calculateAutoSizePost) {
                Vector2 size = widget.layout.calculateAutoSize(widget);
                widget.boundingBox.innerWidth = size.x;
                widget.boundingBox.innerHeight = size.y;
            }
        }
    }
    public void calculatePosition() {
        layout.reset(this);

        for (WWidget widget : widgets) {
            if (widget.boundingBox.calculateAutoSizePost) widget.boundingBox.calculateCustomSize();

            Box box = layout.layoutWidget(this, widget);
            widget.boundingBox.calculatePos(box);

            widget.calculatePosition();
        }
    }
    public void layout() {
        WWidget root = getRoot();
        root.calculateSize();
        root.calculatePosition();
    }

    public Vector2 calculateCustomSize() {
        return Vector2.ZERO;
    }

    public WWidget getRoot() {
        return parent != null ? parent.getRoot() : this;
    }

    public void move(double x, double y) {
        move(this, x, y);
    }
    private void move(WWidget w, double x, double y) {
        w.boundingBox.x = x;
        w.boundingBox.y = y;
        for (WWidget widget : w.widgets) widget.move(x, y);
    }

    public void tick() {
        if (mouseOver) tooltipTimer++;
        onTick();
        for (WWidget widget : widgets) widget.tick();
    }
    public void onTick() {}

    public void render(double delta) {
        if (!visible) return;
        onRender(delta);
        for (WWidget widget : widgets) widget.render(delta);
    }
    public void onRender(double delta) {}

    public void renderPost(double delta, double mouseX, double mouseY) {
        if (!visible) return;
        onRenderPost(delta);
        for (WWidget widget : widgets) widget.renderPost(delta, mouseX, mouseY);
    }
    public void onRenderPost(double delta) {
    }

    public void renderTooltip(double mouseX, double mouseY) {
        if (!visible) return;
        if (tooltipTimer >= 20 && tooltip != null && mouseOver) {
            Utils.drawTextWithShadow(tooltip, (float) mouseX + 8, (float) mouseY + 8, GUI.textC);
            return;
        }
        for (WWidget widget : widgets) widget.renderTooltip(mouseX, mouseY);
    }

    protected void renderBackground(Color background, Color outline) {
        RenderUtils.quad(boundingBox.x, boundingBox.y, boundingBox.getWidth(), boundingBox.getHeight(), background);
        RenderUtils.quad(boundingBox.x, boundingBox.y, boundingBox.getWidth(), 1, outline);
        RenderUtils.quad(boundingBox.x, boundingBox.y + boundingBox.getHeight(), boundingBox.getWidth(), -1, outline);
        RenderUtils.quad(boundingBox.x, boundingBox.y, 1, boundingBox.getHeight(), outline);
        RenderUtils.quad(boundingBox.x + boundingBox.getWidth(), boundingBox.y, -1, boundingBox.getHeight(), outline);
    }

    public static class DefaultLayout extends WidgetLayout {
        protected Box box = new Box();

        @Override
        public void reset(WWidget widget) {
            box.x = widget.boundingBox.getInnerX();
            box.y = widget.boundingBox.getInnerY();
            box.width = widget.boundingBox.innerWidth;
            box.height = widget.boundingBox.innerHeight;
        }

        @Override
        public Vector2 calculateAutoSize(WWidget widget) {
            Vector2 maxSize = new Vector2();

            for (WWidget w : widget.widgets) {
                maxSize.x = Math.max(maxSize.x, w.boundingBox.getWidth());
                maxSize.y = Math.max(maxSize.y, w.boundingBox.getHeight());
            }

            return maxSize;
        }

        @Override
        public Box layoutWidget(WWidget widget, WWidget child) {
            return box;
        }
    }
}
