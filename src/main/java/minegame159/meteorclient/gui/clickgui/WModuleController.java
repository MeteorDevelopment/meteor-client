package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.WidgetLayout;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Box;
import minegame159.meteorclient.utils.Vector2;

public class WModuleController extends WWidget {
    private WModuleGroup grabbing;
    private double lastMouseX, lastMouseY;

    public WModuleController() {
        layout = new ModuleControllerLayout();
        boundingBox.setMargin(16);

        for (Category category : ModuleManager.CATEGORIES) {
            add(new WModuleGroup(category));
        }
    }

    @Override
    public boolean onMousePressed(int button) {
        if (grabbing == null) {
            for (WWidget widget : widgets) {
                if (widget.mouseOver) {
                    grabbing = (WModuleGroup) widget;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onMouseReleased(int button) {
        if (grabbing != null) {
            grabbing = null;
            return true;
        }

        return false;
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY) {
        if (grabbing != null) {
            move(grabbing, mouseX - lastMouseX, mouseY - lastMouseY);
            Vector2 pos = Config.INSTANCE.guiPositions.computeIfAbsent(grabbing.category, category -> new Vector2());
            pos.x = grabbing.boundingBox.x;
            pos.y = grabbing.boundingBox.y;
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    private void move(WWidget widget, double deltaX, double deltaY) {
        widget.boundingBox.x += deltaX;
        widget.boundingBox.y += deltaY;

        for (WWidget w : widget.widgets) move(w, deltaX, deltaY);
    }

    private static class ModuleControllerLayout extends WidgetLayout {
        private Box box = new Box();

        @Override
        public void reset(WWidget widget) {

        }

        @Override
        public Vector2 calculateAutoSize(WWidget widget) {
            return null;
        }

        @Override
        public Box layoutWidget(WWidget widget, WWidget child) {
            Vector2 pos = Config.INSTANCE.guiPositions.get(((WModuleGroup) child).category);

            if (pos != null) {
                box.x = pos.x;
                box.y = pos.y;
            } else box.x += 10 + box.width;

            box.width = child.boundingBox.getWidth();
            box.height = child.boundingBox.getHeight();

            return box;
        }
    }
}
