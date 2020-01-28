package minegame159.meteorclient.clickgui.widgets;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.utils.Vector2;
import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Map;

public class ModuleGroupController extends Widget {
    private Map<Category, ModuleGroup> groups = new HashMap<>();

    private Category dragging;
    private double lastMouseX, lastMouseY;

    public ModuleGroupController() {
        super(0, 0, 0);
    }

    public ModuleGroup addGroup(Category category, Vector2 pos) {
        ModuleGroup group = new ModuleGroup(category);
        groups.put(category, group);
        if (!Config.instance.guiPositions.containsKey(category)) Config.instance.guiPositions.put(category, pos);
        addWidget(group);
        return group;
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (super.onMouseClicked(mouseX, mouseY, button)) return true;

        if (dragging == null && button == 0) {
            for (Category category : groups.keySet()) {
                if (groups.get(category).isMouseOver(mouseX, mouseY)) {
                    dragging = category;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (super.onMouseReleased(mouseX, mouseY, button)) return true;

        if (dragging != null && button == 0) {
            dragging = null;
            return true;
        }

        return false;
    }

    @Override
    public void onMouseMoved(double mouseX, double mouseY) {
        if (dragging != null) {
            ModuleGroup group = groups.get(dragging);

            moveWidget(group, -(lastMouseX - mouseX), -(lastMouseY - mouseY));

            Vector2 pos = Config.instance.guiPositions.get(dragging);
            pos.x = group.x;
            pos.y = group.y;
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;

        super.onMouseMoved(mouseX, mouseY);
    }

    private void moveWidget(Widget widget, double deltaX, double deltaY) {
        widget.x += deltaX;
        widget.y += deltaY;

        for (Widget w : widget.widgets) moveWidget(w, deltaX, deltaY);
    }

    @Override
    public void layout() {
        width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        height = MinecraftClient.getInstance().getWindow().getScaledHeight();

        for (Widget widget : widgets) {
            if (!(widget instanceof ModuleGroup)) continue;
            Vector2 pos = Config.instance.guiPositions.get(((ModuleGroup) widget).category);

            widget.x = pos.x;
            widget.y = pos.y;

            widget.layout();
        }
    }

    @Override
    public void onWindowResized(double windowWidth, double windowHeight) {
        layout();
        super.onWindowResized(windowWidth, windowHeight);
    }
}
