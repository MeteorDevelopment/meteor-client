package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;

public class WWindow extends WVerticalList {
    public interface OnMovedAction {
        public void onMoved(WWindow window);
    }

    public OnMovedAction onDragged;

    private WVerticalList list;

    public WWindow(String title, double horizontalMargin, double spacing) {
        super(spacing);
        maxHeight = MinecraftClient.getInstance().window.getScaledHeight() - 32;
        boundingBox.alignment.set(Alignment.X.Center, Alignment.Y.Center);

        super.add(new Header(title, this));

        list = super.add(new WVerticalList(spacing));
        list.boundingBox.setMargin(horizontalMargin, 0);
        list.boundingBox.marginBottom = 4;
    }

    @Override
    public <T extends WWidget> T add(T widget) {
        return list.add(widget);
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
            boundingBox.setMargin(8);
            boundingBox.fullWidth = true;

            this.window = window;

            WLabel title = add(new WLabel(text, true));
            title.boundingBox.alignment.set(Alignment.X.Center, Alignment.Y.Center);
            title.color = GUI.windowHeaderText;
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
