/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud;

import minegame159.meteorclient.events.render.Render2DEvent;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.render.hud.modules.HudModule;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class HudEditorScreen extends Screen {
    private final Color HOVER_BG_COLOR = new Color(200, 200, 200, 50);
    private final Color HOVER_OL_COLOR = new Color(200, 200, 200, 200);

    private final Color INACTIVE_BG_COLOR = new Color(200, 25, 25, 50);
    private final Color INACTIVE_OL_COLOR = new Color(200, 25, 25, 200);

    private final Screen parent;
    private final HUD hud;

    private boolean selecting;
    private double mouseStartX, mouseStartY;

    private boolean dragging;
    private double lastMouseX, lastMouseY;
    private HudModule hoveredModule;
    private final List<HudModule> selectedModules = new ArrayList<>();

    public HudEditorScreen() {
        super(new LiteralText("Hud Editor"));

        this.parent = MinecraftClient.getInstance().currentScreen;
        this.hud = Modules.get().get(HUD.class);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hoveredModule != null) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) hoveredModule.active = !hoveredModule.active;
            else {
                dragging = true;

                if (!selectedModules.contains(hoveredModule)) {
                    selectedModules.clear();
                    selectedModules.add(hoveredModule);
                }
            }
            return true;
        }

        double s = MinecraftClient.getInstance().getWindow().getScaleFactor();

        selecting = true;
        mouseStartX = mouseX * s;
        mouseStartY = mouseY * s;

        if (!selectedModules.isEmpty()) {
            selectedModules.clear();
            return true;
        }

        return false;
    }

    private boolean isInSelection(double mouseX, double mouseY, double x, double y) {
        double sx, sy;
        double sw, sh;

        if (mouseX >= mouseStartX) {
            sx = mouseStartX;
            sw = mouseX - mouseStartX;
        } else {
            sx = mouseX;
            sw = mouseStartX - mouseX;
        }

        if (mouseY >= mouseStartY) {
            sy = mouseStartY;
            sh = mouseY - mouseStartY;
        } else {
            sy = mouseY;
            sh = mouseStartY - mouseY;
        }

        return x >= sx && x <= sx + sw && y >= sy && y <= sy + sh;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        double s = MinecraftClient.getInstance().getWindow().getScaleFactor();

        mouseX *= s;
        mouseY *= s;

        if (selecting) {
            selectedModules.clear();

            for (HudModule module : hud.modules) {
                int mX = module.box.getX();
                int mY = module.box.getY();
                int mW = module.box.width;
                int mH = module.box.height;

                if (isInSelection(mouseX, mouseY, mX, mY) || isInSelection(mouseX, mouseY, mX + mW, mY) || (isInSelection(mouseX, mouseY, mX, mY + mH) || isInSelection(mouseX, mouseY, mX + mW, mY + mH))) {
                    selectedModules.add(module);
                }
            }
        } else if (dragging) {
            for (HudModule module : selectedModules) {
                module.box.addPos(mouseX - lastMouseX, mouseY - lastMouseY);
            }
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            selectedModules.clear();
            return true;
        }

        if (selecting) {
            selecting = false;
            return true;
        }

        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        double s = MinecraftClient.getInstance().getWindow().getScaleFactor();

        mouseX *= s;
        mouseY *= s;

        if (!Utils.canUpdate()) {
            renderBackground(matrices);

            Utils.unscaledProjection();
            hud.onRender(Render2DEvent.get(0, 0, delta));
        }
        else {
            Utils.unscaledProjection();
        }

        Renderer.NORMAL.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);

        for (HudModule module : hud.modules) {
            if (module.active) continue;

            renderModule(module, INACTIVE_BG_COLOR, INACTIVE_OL_COLOR);
        }

        for (HudModule module : selectedModules) {
            renderModule(module, HOVER_BG_COLOR, HOVER_OL_COLOR);
        }

        if (!dragging) {
            hoveredModule = null;

            for (HudModule module : hud.modules) {
                if (module.box.isOver(mouseX, mouseY)) {
                    if (!selectedModules.contains(module)) renderModule(module, HOVER_BG_COLOR, HOVER_OL_COLOR);
                    hoveredModule = module;

                    break;
                }
            }

            if (selecting) {
                renderQuad(mouseStartX, mouseStartY, mouseX - mouseStartX, mouseY - mouseStartY, HOVER_BG_COLOR, HOVER_OL_COLOR);
            }
        }

        Renderer.NORMAL.end();
        Utils.scaledProjection();
    }

    private void renderModule(HudModule module, Color bgColor, Color olColor) {
        renderQuad(module.box.getX(), module.box.getY(), module.box.width, module.box.height, bgColor, olColor);
    }

    private void renderQuad(double x, double y, double w, double h, Color bgColor, Color olColor) {
        Renderer.NORMAL.quad(x, y, w, h, bgColor);
        Renderer.NORMAL.quad(x - 1, y - 1, w + 2, 1, olColor);
        Renderer.NORMAL.quad(x - 1, y + h - 1, w + 2, 1, olColor);
        Renderer.NORMAL.quad(x - 1, y, 1, h, olColor);
        Renderer.NORMAL.quad(x + w, y, 1, h, olColor);
    }

    @Override
    public void onClose() {
        MinecraftClient.getInstance().openScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
