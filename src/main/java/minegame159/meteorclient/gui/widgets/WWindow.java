package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.listeners.WindowDragListener;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class WWindow extends WTable {
    public WindowDragListener onDragged;
    public final GuiConfig.WindowType type;

    private final String title;
    private WTable table;

    private boolean expanded;
    private Header header;

    private double padding, spacing;

    private boolean wasMoved;
    private double mX, mY;

    public WWindow(String title, boolean expanded, double padding, double spacing, GuiConfig.WindowType type) {
        this.title = title;
        this.expanded = expanded;
        this.padding = padding;
        this.spacing = spacing;
        this.type = type;

        defaultCell.space(0);
        initWidgets();

        if (type != null) {
            setExpanded(GuiConfig.INSTANCE.getWindowConfig(type, expanded).isExpanded());
        }
    }

    public WWindow(String title, boolean expanded, GuiConfig.WindowType type) {
        this(title, expanded, 6, 4, type);
    }

    public WWindow(String title, boolean expanded) {
        this(title, expanded, null);
    }

    @Override
    public <T extends WWidget> Cell<T> add(T widget) {
        return table.add(widget);
    }

    @Override
    public void row() {
        table.row();
    }

    @Override
    public void clear() {
        table.clear();
    }

    private void initWidgets() {
        header = super.add(new Header()).fillX().expandX().getWidget();
        super.row();

        table = super.add(new WTable()).fillX().expandX().getWidget();
        table.pad(padding);
        table.defaultCell.space(spacing);
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        header.triangle.setChecked(!expanded);
    }

    @Override
    public boolean mouseClicked(int button) {
        for (Cell<?> cell : cells) {
            if ((expanded || cell.getWidget() instanceof Header) && cell.getWidget().mouseClicked(button)) return true;
        }
        return onMouseClicked(button);
    }

    @Override
    public boolean mouseReleased(int button) {
        for (Cell<?> cell : cells) {
            if ((expanded || cell.getWidget() instanceof Header) && cell.getWidget().mouseReleased(button)) return true;
        }
        return onMouseReleased(button);
    }

    @Override
    public void mouseMoved(double x, double y) {
        for (Cell<?> cell : cells) {
            if (expanded || cell.getWidget() instanceof Header) cell.getWidget().mouseMoved(x, y);
        }
        boolean preMouseOver = mouseOver;
        mouseOver = isOver(x, y);
        if (preMouseOver && mouseOver) mouseOverTimer = 0;
        onMouseMoved(x, y);
    }

    @Override
    public boolean mouseScrolled(double amount) {
        for (Cell<?> cell : cells) {
            if ((expanded || cell.getWidget() instanceof Header) && cell.getWidget().mouseScrolled(amount)) return true;
        }
        return onMouseScrolled(amount);
    }

    @Override
    public boolean keyPressed(int key, int mods) {
        for (Cell<?> cell : cells) {
            if ((expanded || cell.getWidget() instanceof Header) && cell.getWidget().keyPressed(key, mods)) return true;
        }
        return onKeyPressed(key, mods);
    }

    @Override
    public boolean keyRepeated(int key, int mods) {
        for (Cell<?> cell : cells) {
            if ((expanded || cell.getWidget() instanceof Header) && cell.getWidget().keyRepeated(key, mods)) return true;
        }
        return onKeyRepeated(key, mods);
    }

    @Override
    public boolean charTyped(char c, int key) {
        for (Cell<?> cell : cells) {
            if ((expanded || cell.getWidget() instanceof Header) && cell.getWidget().charTyped(c, key)) return true;
        }
        return onCharTyped(c, key);
    }

    @Override
    protected void onCalculateSize() {
        maxHeight = MinecraftClient.getInstance().window.getScaledHeight() - 32;
        animationProgress = expanded ? 1 : 0;

        super.onCalculateSize();
    }

    @Override
    protected void onCalculateWidgetPositions() {
        super.onCalculateWidgetPositions();

        if (wasMoved) {
            move(mX - x, mY - y, true);
        }
    }

    @Override
    public void render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        animationProgress += delta / 4 * (expanded ? 1 : -1);
        animationProgress = Utils.clamp(animationProgress, header.height / height, 1);

        super.render(renderer, mouseX, mouseY, delta);
    }

    @Override
    protected void onRenderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animationProgress > header.height / height) widget.render(renderer, mouseX, mouseY, delta);
        else {
            if (widget instanceof Header) widget.render(renderer, mouseX, mouseY, delta);
        }
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animationProgress > 0) renderer.renderQuad(x, y, width, height, GuiConfig.INSTANCE.background);
    }

    private class Header extends WTable {
        WLabel label;
        WTriangle triangle;

        boolean dragging;
        double lastMouseX, lastMouseY;

        Header() {
            pad(4);

            label = add(new WLabel(title, true)).fillX().centerX().padRight(4).getWidget();

            triangle = add(new WTriangle()).getWidget();
            triangle.action = triangle1 -> {
                expanded = !triangle1.checked;
                if (type != null) GuiConfig.INSTANCE.getWindowConfig(type, false).setExpanded(!triangle1.checked);
            };
        }

        @Override
        protected boolean onMouseClicked(int button) {
            if (mouseOver && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                dragging = true;
                return true;
            }

            return false;
        }

        @Override
        protected boolean onMouseReleased(int button) {
            if (mouseOver) {
                dragging = false;

                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    triangle.mouseOver = true;
                    triangle.onMouseReleased(button);
                }

                return true;
            }

            return false;
        }

        @Override
        protected void onMouseMoved(double x, double y) {
            if (dragging) {
                WWindow.this.move(x - lastMouseX, y - lastMouseY, false);
                mX = WWindow.this.x;
                mY = WWindow.this.y;
                wasMoved = true;

                if (onDragged != null) onDragged.onWindowDrag(WWindow.this);
            }

            lastMouseX = x;
            lastMouseY = y;
        }

        @Override
        protected void onCalculateWidgetPositions() {
            super.onCalculateWidgetPositions();
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.renderQuad(x, y, width, height, GuiConfig.INSTANCE.accent);
        }
    }
}
