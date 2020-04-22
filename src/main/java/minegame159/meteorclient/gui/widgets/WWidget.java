package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.gui.GuiRenderer;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class WWidget {
    public boolean visible = true;

    public double x, y;
    public double width, height;

    protected WWidget parent;
    protected List<Cell<?>> cells = new ArrayList<>();

    private boolean needsLayout;
    protected boolean mouseOver;
    protected double mouseOverTimer;
    public String tooltip;

    public void invalidate() {
        getRoot().needsLayout = true;
    }

    public <T extends WWidget> Cell<T> add(T widget) {
        widget.parent = this;
        Cell<T> cell = new Cell<>();
        cell.widget = widget;
        cells.add(cell);
        invalidate();
        return cell;
    }

    public <T extends WWidget> void remove(T widget) {
        Cell<T> temp = new Cell<>();
        temp.widget = widget;
        if (cells.remove(temp)) invalidate();
    }

    public void clear() {
        if (cells.size() > 0) {
            cells.clear();
            invalidate();
        }
    }

    public boolean isOver(double x, double y) {
        return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
    }

    public void move(double deltaX, double deltaY, boolean callMouseMoved) {
        move(this, deltaX, deltaY);
        if (callMouseMoved) mouseMoved(MinecraftClient.getInstance().mouse.getX() / MinecraftClient.getInstance().window.getScaleFactor(), MinecraftClient.getInstance().mouse.getY() / MinecraftClient.getInstance().window.getScaleFactor());
    }
    protected void move(WWidget widget, double deltaX, double deltaY) {
        widget.x += deltaX;
        widget.y += deltaY;

        for (Cell<?> cell : widget.cells) {
            cell.x += deltaX;
            cell.y += deltaY;
            move(cell.getWidget(), deltaX, deltaY);
        }
    }

    public void layout() {
        // Calculate size from top to bottom
        calculateSize();

        // Calculate widget positions from bottom to top
        calculateWidgetPositions();

        mouseMoved(MinecraftClient.getInstance().mouse.getX() / MinecraftClient.getInstance().window.getScaleFactor(), MinecraftClient.getInstance().mouse.getY() / MinecraftClient.getInstance().window.getScaleFactor());
        setNeedsLayout(this, false);
    }
    private void setNeedsLayout(WWidget widget, boolean needsLayout) {
        widget.needsLayout = needsLayout;
        for (Cell<?> cell : widget.cells) setNeedsLayout(cell.getWidget(), needsLayout);
    }

    private void calculateWidgetPositions() {
        onCalculateWidgetPositions();
        for (Cell cell : cells) cell.getWidget().calculateWidgetPositions();
    }
    protected void onCalculateWidgetPositions() {
        for (Cell<?> cell : cells) {
            cell.x = x + cell.padLeft;
            cell.y = y + cell.padTop;
            cell.width = width - cell.padLeft - cell.padRight;
            cell.height = height - cell.padTop - cell.padBottom;
            cell.alignWidget();
        }
    }

    private void calculateSize() {
        for (Cell cell : cells) cell.getWidget().calculateSize();
        onCalculateSize();
    }
    protected void onCalculateSize() {
        width = 10;
        height = 10;
    }

    public void tick() {
        if (!visible) return;
        onTick();
        for (Cell<?> cell : cells) cell.getWidget().tick();
    }
    protected void onTick() {}

    public void render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!visible) return;
        if (needsLayout) layout();
        if (mouseOver) mouseOverTimer += delta / 10;
        onRender(renderer, mouseX, mouseY, delta);
        for (Cell<?> cell : cells) {
            if (cell.x > MinecraftClient.getInstance().window.getScaledWidth() || cell.y > MinecraftClient.getInstance().window.getScaledHeight()) break;
            onRenderWidget(cell.getWidget(), renderer, mouseX, mouseY, delta);
        }
        if (mouseOver && mouseOverTimer >= 1 && tooltip != null) renderer.renderText(tooltip, mouseX + 8, mouseY + 8, GUI.text, true);
    }
    protected void onRenderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        widget.render(renderer, mouseX, mouseY, delta);
    }
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {}

    public WWidget getRoot() {
        return parent != null ? parent.getRoot() : this;
    }

    public List<Cell<?>> getCells() {
        return cells;
    }

    public void mouseMoved(double x, double y) {
        for (Cell<?> cell : cells) cell.getWidget().mouseMoved(x, y);
        boolean preMouseOver = mouseOver;
        mouseOver = isOver(x, y);
        if (!preMouseOver && mouseOver) mouseOverTimer = 0;
        onMouseMoved(x, y);
    }
    protected void onMouseMoved(double x, double y) {}

    public boolean mouseClicked(int button) {
        for (Cell<?> cell : cells) {
            if (cell.getWidget().mouseClicked(button)) return true;
        }
        return onMouseClicked(button);
    }
    protected boolean onMouseClicked(int button) { return false; }

    public boolean mouseReleased(int button) {
        for (Cell<?> cell : cells) {
            if (cell.getWidget().mouseReleased(button)) return true;
        }
        return onMouseReleased(button);
    }
    protected boolean onMouseReleased(int button) { return false; }

    public boolean mouseScrolled(double amount) {
        for (Cell<?> cell : cells) {
            if (cell.getWidget().mouseScrolled(amount)) return true;
        }
        return onMouseScrolled(amount);
    }
    protected boolean onMouseScrolled(double amount) { return false; }

    public boolean keyPressed(int key, int mods) {
        for (Cell<?> cell : cells) {
            if (cell.getWidget().keyPressed(key, mods)) return true;
        }
        return onKeyPressed(key, mods);
    }
    protected boolean onKeyPressed(int key, int mods) { return false; }

    public boolean keyRepeated(int key, int mods) {
        for (Cell<?> cell : cells) {
            if (cell.getWidget().keyRepeated(key, mods)) return true;
        }
        return onKeyRepeated(key, mods);
    }
    protected boolean onKeyRepeated(int key, int mods) { return false; }

    public boolean charTyped(char c, int key) {
        for (Cell<?> cell : cells) {
            if (cell.getWidget().charTyped(c, key)) return true;
        }
        return onCharTyped(c, key);
    }
    protected boolean onCharTyped(char c, int key) { return false; }
}
