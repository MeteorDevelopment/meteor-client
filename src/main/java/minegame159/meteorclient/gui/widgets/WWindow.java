package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;

public class WWindow extends WVerticalList {
    public interface OnMovedAction {
        public void onMoved(WWindow window);
    }

    public OnMovedAction onDragged;

    private boolean expanded;
    private Config.WindowType type;
    private Header header;
    private WVerticalList list;

    public WWindow(String title, Config.WindowType type, double horizontalMargin, double spacing, boolean expanded) {
        super(spacing);
        maxHeight = MinecraftClient.getInstance().window.getScaledHeight() - 32;
        boundingBox.alignment.set(Alignment.X.Center, Alignment.Y.Center);

        if (type != null) this.expanded = Config.INSTANCE.getWindowConfig(type, expanded).isExpanded();
        else this.expanded = expanded;

        this.type = type;

        header = super.add(new Header(title, this));

        list = super.add(new WVerticalList(spacing));
        list.boundingBox.setMargin(horizontalMargin, 0);
        list.boundingBox.marginBottom = 4;
    }

    @Override
    public <T extends WWidget> T add(T widget) {
        return list.add(widget);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public Config.WindowType getType() {
        return type;
    }

    @Override
    public boolean mousePressed(int button) {
        for (WWidget widget : widgets) {
            if ((expanded || widget == header) && widget.mousePressed(button)) return true;
        }
        return onMousePressed(button);
    }

    @Override
    public boolean mouseReleased(int button) {
        for (WWidget widget : widgets) {
            if ((expanded || widget == header) && widget.mouseReleased(button)) return true;
        }
        return onMouseReleased(button);
    }

    @Override
    public boolean mouseScrolled(double amount) {
        for (WWidget widget : widgets) {
            if ((expanded || widget == header) && widget.mouseScrolled(amount)) return true;
        }
        return onMouseScrolled(amount);
    }

    @Override
    public void mouseMove(double mouseX, double mouseY) {
        boolean lastMoveOver = mouseOver;
        mouseOver = boundingBox.isOver(mouseX, mouseY);
        if (!lastMoveOver && mouseOver) tooltipTimer = 0;

        onMouseMove(mouseX, mouseY);
        for (WWidget widget : widgets) {
            if (expanded || widget == header) widget.mouseMove(mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(int key, int modifiers) {
        for (WWidget widget : widgets) {
            if ((expanded || widget == header) && widget.keyPressed(key, modifiers)) return true;
        }
        return onKeyPressed(key, modifiers);
    }

    @Override
    public boolean keyRepeated(int key) {
        for (WWidget widget : widgets) {
            if ((expanded || widget == header) && widget.keyRepeated(key)) return true;
        }
        return onKeyRepeated(key);
    }

    @Override
    public boolean charTyped(char c, int key) {
        for (WWidget widget : widgets) {
            if ((!expanded || widget == header) && widget.charTyped(c, key)) return true;
        }
        return onCharTyped(c, key);
    }

    @Override
    public void render(double delta) {
        if (expanded) super.render(delta);
        else {
            if (!visible) return;
            for (WWidget widget : widgets) {
                if (widget.boundingBox.y > MinecraftClient.getInstance().window.getScaledHeight()) break;
                if (!expanded) if (widget != header) continue;
                widget.render(delta);
            }
        }
    }

    @Override
    public void renderPost(double delta, double mouseX, double mouseY) {
        if (expanded) super.renderPost(delta, mouseX, mouseY);
        else {
            if (!visible) return;
            for (WWidget widget : widgets) {
                if (widget.boundingBox.y > MinecraftClient.getInstance().window.getScaledHeight()) break;
                if (!expanded) if (widget != header) continue;
                widget.renderPost(delta, mouseX, mouseY);
            }
        }
    }

    @Override
    public void onRender(double delta) {
        RenderUtils.quad(boundingBox.x, boundingBox.y, boundingBox.getWidth(), boundingBox.getHeight(), GUI.background);
    }

    @Override
    public void onWindowResized(int width, int height) {
        list.maxHeight = height - 32;
        list.calculateSize();
        list.calculatePosition();
    }

    private static class Header extends WWidget {
        private WWindow window;
        private boolean dragging;
        private double lastMouseX, lastMouseY;

        public Header(String text, WWindow window) {
            boundingBox.autoSize = true;
            boundingBox.fullWidth = true;
            boundingBox.setMargin(4);

            this.window = window;

            WHorizontalList hList = add(new WHorizontalList(4));
            hList.boundingBox.fullWidth = true;
            hList.boundingBox.alignment.set(Alignment.X.Center, Alignment.Y.Center);

            WLabel title = hList.add(new WLabel(text, true));
            title.color = GUI.windowHeaderText;

            WCheckbox checkbox = hList.add(new WCheckbox(window.expanded));
            checkbox.boundingBox.alignment.set(Alignment.X.Right, Alignment.Y.Center);
            checkbox.boundingBox.setMargin(1);
            checkbox.size = 8;
            checkbox.setAction(wCheckbox -> {
                window.expanded = wCheckbox.checked;
                if (window.type != null) Config.INSTANCE.getWindowConfig(window.type, false).setExpanded(wCheckbox.checked);
            });
        }

        @Override
        public boolean onMousePressed(int button) {
            if (mouseOver && button == 0) {
                dragging = true;
                return true;
            }

            return false;
        }

        @Override
        public boolean onMouseReleased(int button) {
            if (dragging && button == 0) {
                dragging = false;
                return true;
            }

            return false;
        }

        @Override
        public void onMouseMove(double mouseX, double mouseY) {
            if (dragging) {
                window.move(mouseX - lastMouseX, mouseY - lastMouseY);
                if (window.onDragged != null) window.onDragged.onMoved(window);
            }

            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        @Override
        public void onRender(double delta) {
            RenderUtils.quad(boundingBox.x, boundingBox.y, boundingBox.getWidth(), boundingBox.getHeight(), GUI.accent);
        }
    }
}
