package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.Utils;

public class WCollapsableTable extends WTable {
    private final String title;
    public final Header header;
    public final WTable table;

    public boolean expanded = false;
    private double fullHeight;

    public WCollapsableTable(String title) {
        this.title = title;

        header = super.add(new Header()).fillX().expandX().getWidget();
        super.row();

        table = super.add(new WTable()).fillX().expandX().getWidget();
        table.pad(4);
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
        maxHeight = Utils.getScaledWindowHeightGui() - 32;

        super.onCalculateSize();

        fullHeight = height;
        if (!expanded) height = header.height;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        header.triangle.checked = !expanded;
        if (expanded) height = fullHeight;
        else height = header.height;
        invalidate();
    }

    @Override
    protected void onRenderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded) widget.render(renderer, mouseX, mouseY, delta);
        else {
            if (widget instanceof Header) widget.render(renderer, mouseX, mouseY, delta);
        }
    }

    public class Header extends WTable {
        private final WTriangle triangle;

        Header() {
            pad(4);

            triangle = add(new WTriangle()).getWidget();
            triangle.accentColor = true;
            triangle.checked = !expanded;
            triangle.action = triangle1 -> setExpanded(!triangle1.checked);

            add(new WLabel(title, true)).fillX().centerX().padRight(4);
        }

        @Override
        protected boolean onMouseReleased(int button) {
            if (mouseOver) {
                triangle.pressed = true;
                triangle.onMouseReleased(button);

                return true;
            }

            return false;
        }
    }
}
