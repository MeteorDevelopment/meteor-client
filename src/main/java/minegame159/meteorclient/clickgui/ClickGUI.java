package minegame159.meteorclient.clickgui;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.Vector2;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {
    private static int bgColor = Color.fromRGBA(80, 80, 80, 80);

    private List<ModuleGroup> groups = new ArrayList<>();
    private ModuleGroup hovering;
    private ModuleGroup dragging;
    private double lastMouseX, lastMouseY;

    public ClickGUI() {
        super(new LiteralText("ClickGUI"));

        double titleOffset = 4;

        double height = Utils.getTextHeight() + titleOffset * 2;
        double x = 16;
        double y = 16;

        for (Category category : ModuleManager.getCategories()) {
            double width = 0;
            Module module = null;
            for (Module mod : ModuleManager.getGroup(category)) {
                if (mod.title.length() > width) {
                    width = mod.title.length();
                    module = mod;
                }
            }
            width = Utils.getTextWidth(module.title) + 8 + 8;

            if (Config.instance.guiPositions.containsKey(category)) {
                Vector2 pos = Config.instance.guiPositions.get(category);
                x = pos.x;
                y = pos.y;
            }

            groups.add(new ModuleGroup(category, x, y, width, height, titleOffset));

            x += width + 8;
            if (x + width >= MinecraftClient.getInstance().getWindow().getScaledWidth()) {
                x = 16;
                y += 16 + 8;
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        renderBackground();

        RenderUtils.beginQuads();
        for (ModuleGroup group : groups) group.render(mouseX, mouseY);
        RenderUtils.endQuads();

        for (ModuleGroup group : groups) group.renderText(font);

        super.render(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dragging == null) {
            for (ModuleGroup group : groups) {
                if (group.isMouseOver(mouseX, mouseY)) {
                    dragging = group;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            return true;
        } else {
            for (ModuleGroup group : groups) {
                if (group.mouseClicked(mouseX, mouseY, button)) break;
            }
        }

        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (dragging != null) dragging.mouseDragged(-(lastMouseX - mouseX), -(lastMouseY - mouseY));

        if (hovering == null) {
            for (ModuleGroup group : groups) {
                if (group.isMouseOver(mouseX, mouseY)) {
                    hovering = group;
                    hovering.highlighted = true;
                    break;
                }
            }
        } else {
            if (!hovering.isMouseOver(mouseX, mouseY)) {
                hovering.highlighted = false;
                hovering = null;
            }
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    @Override
    public void renderBackground() {
        fill(0, 0, width, height, bgColor);
    }

    @Override
    public void removed() {
        for (ModuleGroup group : groups) {
            Vector2 pos = new Vector2(group.x, group.y);
            Config.instance.guiPositions.put(group.category, pos);
        }
    }
}
