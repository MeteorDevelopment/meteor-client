package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;

public class WPanel extends WWidget {
    public interface Action {
        public void onDragged(WPanel panel);
    }

    public Action onDragged;

    private boolean dragging;
    private double lastMouseX, lastMouseY;

    public WPanel() {
        boundingBox.autoSize = true;
    }

    @Override
    public boolean onMousePressed(int button) {
        if (mouseOver && !dragging && button == 0) {
            dragging = true;
            return true;
        }

        return false;
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        if (dragging) {
            move(mouseX - lastMouseX, mouseY - lastMouseY);
            if (onDragged != null) onDragged.onDragged(this);
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    @Override
    public boolean onMouseReleased(int button) {
        if (mouseOver && dragging && button == 0) {
            dragging = false;
            return true;
        }

        return false;
    }

    @Override
    public void onRender(double delta) {
        renderBackground(GUI.background, GUI.outline);
    }
}
