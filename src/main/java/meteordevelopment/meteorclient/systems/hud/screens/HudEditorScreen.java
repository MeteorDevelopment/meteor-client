/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.tabs.builtin.HudTab;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.other.Snapper;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class HudEditorScreen extends WidgetScreen implements Snapper.Container {
    private static final Color SPLIT_LINES_COLOR = new Color(255, 255, 255, 75);

    private static final Color INACTIVE_BG_COLOR = new Color(200, 25, 25, 50);
    private static final Color INACTIVE_OL_COLOR = new Color(200, 25, 25, 200);

    private static final Color HOVER_BG_COLOR = new Color(200, 200, 200, 50);
    private static final Color HOVER_OL_COLOR = new Color(200, 200, 200, 200);

    private static final Color SELECTION_BG_COLOR = new Color(225, 225, 225, 25);
    private static final Color SELECTION_OL_COLOR = new Color(225, 225, 225, 100);

    private final Hud hud;

    private final Snapper snapper;
    private Snapper.Element selectionSnapBox;

    private int lastMouseX, lastMouseY;

    private boolean pressed;
    private int clickX, clickY;
    private final List<HudElement> selection = new ArrayList<>();
    private boolean moved, dragging;
    private HudElement addedHoveredToSelectionWhenClickedElement;

    private double splitLinesAnimation;

    public HudEditorScreen(GuiTheme theme) {
        super(theme, "Hud Editor");

        hud = Hud.get();
        snapper = new Snapper(this);
    }

    @Override
    public void initWidgets() {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            pressed = true;
            selectionSnapBox = null;

            int mX = (int) mc.mouse.getX();
            int mY = (int) mc.mouse.getY();

            HudElement hovered = getHovered(mX, mY);
            dragging = hovered != null;
            if (dragging) {
                if (!selection.contains(hovered)) {
                    selection.clear();
                    selection.add(hovered);
                    addedHoveredToSelectionWhenClickedElement = hovered;
                }
            }
            else selection.clear();

            clickX = mX;
            clickY = mY;
        }

        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        int mX = (int) mc.mouse.getX();
        int mY = (int) mc.mouse.getY();

        if (dragging && !selection.isEmpty()) {
            if (selectionSnapBox == null) selectionSnapBox = new SelectionBox();
            snapper.move(selectionSnapBox, mX - lastMouseX, mY - lastMouseY);
        }

        if (pressed) moved = true;

        lastMouseX = mX;
        lastMouseY = mY;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) pressed = false;

        int mX = (int) mc.mouse.getX();
        int mY = (int) mc.mouse.getY();

        if (addedHoveredToSelectionWhenClickedElement != null) {
            selection.remove(addedHoveredToSelectionWhenClickedElement);
            addedHoveredToSelectionWhenClickedElement = null;
        }

        if (moved) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !dragging) fillSelection();
        }
        else {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                HudElement hovered = getHovered(mX, mY);
                if (hovered != null) hovered.toggle();
            }
            else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                HudElement hovered = getHovered(mX, mY);

                if (hovered != null) mc.setScreen(new HudElementScreen(theme, hovered));
                else mc.setScreen(new AddHudElementScreen(theme, lastMouseX, lastMouseY));
            }
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            snapper.unsnap();
            moved = dragging = false;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!pressed) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                HudElement hovered = getHovered(lastMouseX, lastMouseY);
                if (hovered != null) hovered.toggle();
            }
            else if (keyCode == GLFW.GLFW_KEY_DELETE) {
                HudElement hovered = getHovered(lastMouseX, lastMouseY);

                if (hovered != null) hovered.remove();
                else {
                    for (HudElement element : selection) element.remove();
                    selection.clear();
                }
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void fillSelection() {
        int mX = (int) mc.mouse.getX();
        int mY = (int) mc.mouse.getY();

        int x1 = Math.min(clickX, mX);
        int x2 = Math.max(clickX, mX);

        int y1 = Math.min(clickY, mY);
        int y2 = Math.max(clickY, mY);

        for (HudElement e : hud) {
            if ((e.getX() <= x2 && e.getX2() >= x1) && (e.getY() <= y2 && e.getY2() >= y1)) selection.add(e);
        }
    }

    @Override
    public Iterable<Snapper.Element> getElements() {
        return () -> new Iterator<>() {
            private final Iterator<HudElement> it = hud.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Snapper.Element next() {
                return it.next();
            }
        };
    }

    @Override
    public boolean shouldNotSnapTo(Snapper.Element element) {
        return selection.contains((HudElement) element);
    }

    @Override
    public int getSnappingRange() {
        return hud.snappingRange.get();
    }

    private void onRender(int mouseX, int mouseY) {
        // Inactive
        for (HudElement element : hud) {
            if (!element.isActive()) renderElement(element, INACTIVE_BG_COLOR, INACTIVE_OL_COLOR);
        }

        // Selected
        if (pressed && !dragging) fillSelection();
        for (HudElement element : selection) renderElement(element, HOVER_BG_COLOR, HOVER_OL_COLOR);
        if (pressed && !dragging) selection.clear();

        // Selection
        if (pressed && !dragging) {
            int x1 = Math.min(clickX, mouseX);
            int x2 = Math.max(clickX, mouseX);

            int y1 = Math.min(clickY, mouseY);
            int y2 = Math.max(clickY, mouseY);

            renderQuad(x1, y1, x2 - x1, y2 - y1, SELECTION_BG_COLOR, SELECTION_OL_COLOR);
        }

        // Hovered
        if (!pressed) {
            HudElement hovered = getHovered(mouseX, mouseY);
            if (hovered != null) renderElement(hovered, HOVER_BG_COLOR, HOVER_OL_COLOR);
        }
    }

    private HudElement getHovered(int mouseX, int mouseY) {
        for (HudElement element : hud) {
            if (mouseX >= element.x && mouseX <= element.x + element.getWidth() && mouseY >= element.y && mouseY <= element.y + element.getHeight()) return element;
        }

        return null;
    }

    private void renderQuad(double x, double y, double w, double h, Color bgColor, Color olColor) {
        Renderer2D.COLOR.quad(x + 1, y + 1, w - 2, h - 2, bgColor);

        Renderer2D.COLOR.quad(x, y, w, 1, olColor);
        Renderer2D.COLOR.quad(x, y + h - 1, w, 1, olColor);
        Renderer2D.COLOR.quad(x, y + 1, 1, h - 2, olColor);
        Renderer2D.COLOR.quad(x + w - 1, y + 1, 1, h - 2, olColor);
    }

    private void renderElement(HudElement element, Color bgColor, Color olColor) {
        renderQuad(element.x, element.y, element.getWidth(), element.getHeight(), bgColor, olColor);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!Utils.canUpdate()) renderBackground(matrices);

        double s = mc.getWindow().getScaleFactor();

        mouseX *= s;
        mouseY *= s;

        Utils.unscaledProjection();

        boolean renderSplitLines = pressed && !selection.isEmpty() && moved;
        if (renderSplitLines || splitLinesAnimation > 0) renderSplitLines(renderSplitLines, delta / 20);
        renderElements();

        Renderer2D.COLOR.begin();
        onRender(mouseX, mouseY);
        Renderer2D.COLOR.render(new MatrixStack());

        Utils.scaledProjection();
        runAfterRenderTasks();
    }

    public static void renderElements() {
        Hud hud = Hud.get();
        boolean inactiveOnly = Utils.canUpdate() && hud.active;

        HudRenderer.INSTANCE.begin();

        for (HudElement element : hud) {
            element.updatePos();

            if (inactiveOnly) {
                if (!element.isActive()) element.render(HudRenderer.INSTANCE);
            }
            else element.render(HudRenderer.INSTANCE);
        }

        HudRenderer.INSTANCE.end();
    }

    private void renderSplitLines(boolean increment, double delta) {
        if (increment) splitLinesAnimation += delta * 6;
        else splitLinesAnimation -= delta * 6;
        splitLinesAnimation = Utils.clamp(splitLinesAnimation, 0, 1);

        Renderer2D renderer = Renderer2D.COLOR;
        renderer.begin();

        double w = Utils.getWindowWidth();
        double h = Utils.getWindowHeight();
        double w3 = w / 3.0;
        double h3 = h / 3.0;

        int prevA = SPLIT_LINES_COLOR.a;
        SPLIT_LINES_COLOR.a *= splitLinesAnimation;

        renderSplitLine(renderer, w3, 0, w3, h);
        renderSplitLine(renderer, w3 * 2, 0, w3 * 2, h);

        renderSplitLine(renderer, 0, h3, w, h3);
        renderSplitLine(renderer, 0, h3 * 2, w, h3 * 2);

        SPLIT_LINES_COLOR.a = prevA;

        renderer.render(new MatrixStack());
    }

    private void renderSplitLine(Renderer2D renderer, double x, double y, double destX, double destY) {
        double incX = 0;
        double incY = 0;

        if (x == destX) incY = Utils.getWindowWidth() / 25.0;
        else incX = Utils.getWindowWidth() / 25.0;

        do {
            renderer.line(x, y, x + incX, y + incY, SPLIT_LINES_COLOR);

            x += incX * 2;
            y += incY * 2;

        } while (!(x >= destX) || !(y >= destY));
    }

    public static boolean isOpen() {
        Screen s = mc.currentScreen;
        return s instanceof HudEditorScreen || s instanceof AddHudElementScreen || s instanceof HudElementPresetsScreen || s instanceof HudElementScreen || s instanceof HudTab.HudScreen;
    }

    private class SelectionBox implements Snapper.Element {
        private int x, y;
        private final int width, height;

        public SelectionBox() {
            int x1 = Integer.MAX_VALUE;
            int y1 = Integer.MAX_VALUE;

            int x2 = 0;
            int y2 = 0;

            for (HudElement element : selection) {
                if (element.getX() < x1) x1 = element.getX();
                else if (element.getX() > x2) x2 = element.getX();

                if (element.getX2() < x1) x1 = element.getX2();
                else if (element.getX2() > x2) x2 = element.getX2();

                if (element.getY() < y1) y1 = element.getY();
                else if (element.getY() > y2) y2 = element.getY();

                if (element.getY2() < y1) y1 = element.getY2();
                else if (element.getY2() > y2) y2 = element.getY2();
            }

            this.x = x1;
            this.y = y1;
            this.width = x2 - x1;
            this.height = y2 - y1;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public void setPos(int x, int y) {
            for (HudElement element : selection) element.setPos(x + (element.x - this.x), y + (element.y - this.y));

            this.x = x;
            this.y = y;
        }

        @Override
        public void move(int deltaX, int deltaY) {
            int prevX = x;
            int prevY = y;

            int border = Hud.get().border.get();
            x = Utils.clamp(x + deltaX, border, Utils.getWindowWidth() - width - border);
            y = Utils.clamp(y + deltaY, border, Utils.getWindowHeight() - height - border);

            for (HudElement element : selection) element.move(x - prevX, y - prevY);
        }
    }
}
