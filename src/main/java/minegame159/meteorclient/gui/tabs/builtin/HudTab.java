/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.tabs.builtin;

import minegame159.meteorclient.events.render.Render2DEvent;
import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.WindowScreen;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.tabs.Tab;
import minegame159.meteorclient.gui.tabs.TabScreen;
import minegame159.meteorclient.gui.tabs.WindowTabScreen;
import minegame159.meteorclient.gui.utils.Cell;
import minegame159.meteorclient.gui.widgets.WItemWithLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.gui.widgets.containers.*;
import minegame159.meteorclient.gui.widgets.input.WTextBox;
import minegame159.meteorclient.gui.widgets.pressable.WButton;
import minegame159.meteorclient.gui.widgets.pressable.WCheckbox;
import minegame159.meteorclient.gui.widgets.pressable.WMinus;
import minegame159.meteorclient.gui.widgets.pressable.WPlus;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.systems.hud.ElementRegister;
import minegame159.meteorclient.systems.hud.HUD;
import minegame159.meteorclient.systems.hud.HudElement;
import minegame159.meteorclient.systems.hud.elements.CustomItemHud;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.CursorStyle;
import minegame159.meteorclient.utils.misc.Names;
import minegame159.meteorclient.utils.misc.input.Input;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static minegame159.meteorclient.utils.Utils.getWindowWidth;
import static minegame159.meteorclient.utils.Utils.mc;
import static org.lwjgl.glfw.GLFW.*;

public class HudTab extends Tab {

    public HudTab() {
        super("HUD");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new HudScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof HudScreen;
    }

    public static class HudScreen extends WindowTabScreen {

        public HudScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);

            HUD hud = HUD.get();

            hud.settings.onActivated();
            add(theme.settings(hud.settings)).expandX();

            WButton button = add(theme.button("Editor")).expandX().widget();
            button.action = () -> mc.openScreen(new HudEditorScreen(theme));

            add(theme.horizontalSeparator()).expandX();

            // Bottom
            WHorizontalList bottom = add(theme.horizontalList()).expandX().widget();

            //   Active
            bottom.add(theme.label("Active: "));
            WCheckbox active = bottom.add(theme.checkbox(hud.isActive())).expandCellX().widget();
            active.action = () -> hud.setActive(active.checked);

            WButton reset = bottom.add(theme.button(GuiRenderer.RESET)).right().widget();
            reset.action = hud::reset;
        }

    }

    public static class HudEditorScreen extends WidgetScreen {
        private final HUD hud;

        private boolean selecting;
        private double selectStartX, selectStartY;

        private boolean dragging;
        private double lastMouseX, lastMouseY;

//        private boolean scaling;

        private HudElement hoveredElement;
        private final List<HudElement> selectedElements = new ArrayList<>();

        private final Color BG_COLOR = new Color(200, 200, 200, 50);
        private final Color OL_COLOR = new Color(200, 200, 200, 200);

        public HudEditorScreen(GuiTheme theme) {
            super(theme, "hud-editor");

            this.hud = HUD.get();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double s = mc.getWindow().getScaleFactor();

            mouseX *= s;
            mouseY *= s;

            switch (button) {
                case GLFW_MOUSE_BUTTON_LEFT:
                    if (hoveredElement != null) {
//                        if (isAtElementCorner(hoveredElement, mouseX, mouseY)) {
//                            scaling = true;
//                            selectedElements.clear();
//                            selectedElements.add(hoveredElement);
//                        }
//                        else  {
                            dragging = true;

                            if (!selectedElements.contains(hoveredElement)) {
                                selectedElements.clear();
                                selectedElements.add(hoveredElement);
                            }
//                        }
                        Input.setCursorStyle(CursorStyle.Click);

                        return true;
                    }
                    else {
                        selectedElements.clear();
                        selecting = true;
                        selectStartX = mouseX;
                        selectStartY = mouseY;

                        return false;
                    }
                case GLFW_MOUSE_BUTTON_RIGHT:
                    selectedElements.clear();

                    if (hoveredElement != null) mc.openScreen(new HudElementScreen(theme, hoveredElement));
                    else mc.openScreen(new HudElementSelectorScreen(theme, mouseX, mouseY));

                    return true;
                case GLFW_MOUSE_BUTTON_MIDDLE:
                    if (hoveredElement != null) {
                        selectedElements.remove(hoveredElement);
                        hud.activeElements.remove(hoveredElement);
                        return true;
                    }
                    return false;
            }
            return false;
        }

//        private boolean isAtElementCorner(HudElement element, double mouseX, double mouseY) {
//            if (!(element instanceof ScaleableHudElement)) return false;
//            double bounds = 3 * ((ScaleableHudElement) element).getScale();
//            if ((mouseX > element.box.getX() - bounds && mouseX < element.box.getX() + bounds) && (mouseY > element.box.getY() - bounds && mouseY < element.box.getY() + bounds)) return true;
//            if ((mouseX > element.box.getX() + element.box.width - bounds && mouseX < element.box.getX() + element.box.width + bounds) && (mouseY > element.box.getY() - bounds && mouseY < element.box.getY() + bounds)) return true;
//            if ((mouseX > element.box.getX() - bounds && mouseX < element.box.getX() + bounds) && (mouseY > element.box.getY() + element.box.height - bounds && mouseY < element.box.getY() + element.box.height + bounds)) return true;
//            return (mouseX > element.box.getX() + element.box.width - bounds && mouseX < element.box.getX() + element.box.width + bounds) && (mouseY > element.box.getY() + element.box.height - bounds && mouseY < element.box.getY() + element.box.height + bounds);
//        }

        private boolean isInSelection(double mouseX, double mouseY, double x, double y) {
            double sx, sy;
            double sw, sh;

            if (mouseX >= selectStartX) {
                sx = selectStartX;
                sw = mouseX - selectStartX;
            } else {
                sx = mouseX;
                sw = selectStartX - mouseX;
            }

            if (mouseY >= selectStartY) {
                sy = selectStartY;
                sh = mouseY - selectStartY;
            } else {
                sy = mouseY;
                sh = selectStartY - mouseY;
            }

            return x >= sx && x <= sx + sw && y >= sy && y <= sy + sh;
        }

        @Override
        public void mouseMoved(double mouseX, double mouseY) {
            double s = mc.getWindow().getScaleFactor();

            mouseX *= s;
            mouseY *= s;

            if (selecting) {
                selectedElements.clear();

                for (HudElement element : hud.activeElements) {
                    double mX = element.box.getX();
                    double mY = element.box.getY();
                    double mW = element.box.width;
                    double mH = element.box.height;

                    if (isInSelection(mouseX, mouseY, mX, mY) || isInSelection(mouseX, mouseY, mX + mW, mY) || (isInSelection(mouseX, mouseY, mX, mY + mH) || isInSelection(mouseX, mouseY, mX + mW, mY + mH))) {
                        selectedElements.add(element);
                    }
                }
            }
//            else if (scaling) {
//                if (selectedElements.size() == 1 && selectedElements.get(0) == hoveredElement && hoveredElement instanceof ScaleableHudElement) {
//                    ((ScaleableHudElement) hoveredElement).setScale(mmmjnfenfowehuhfowej);
//                }
//            }
            else if (dragging) {
                for (HudElement element : selectedElements) {
                    element.box.addPos(mouseX - lastMouseX, mouseY - lastMouseY);
                }

                double r = hud.snappingRange.get();

                if (r > 0) {
                    double x = Double.MAX_VALUE;
                    double y = Double.MAX_VALUE;
                    double w = 0;
                    double h = 0;

                    for (HudElement element : selectedElements) {
                        x = Math.min(x, element.box.getX());
                        y = Math.min(y, element.box.getY());
                    }

                    for (HudElement element : selectedElements) {
                        w = Math.max(w, element.box.getX() - x + element.box.width);
                        h = Math.max(h, element.box.getY() - y + element.box.height);
                    }

                    boolean movedX = false;
                    boolean movedY = false;

                    for (HudElement element : hud.activeElements) {
                        if (selectedElements.contains(element)) continue;

                        double eX = element.box.getX();
                        double eY = element.box.getY();
                        double eW = element.box.width;
                        double eH = element.box.height;

                        boolean isHorizontallyIn = isPointBetween(x, w, eX) || isPointBetween(x, w, eX + eW) || isPointBetween(eX, eW, x) || isPointBetween(eX, eW, x + w);
                        boolean isVerticallyIn = isPointBetween(y, h, eY) || isPointBetween(y, h, eY + eH) || isPointBetween(eY, eH, y) || isPointBetween(eY, eH, y + h);

                        double moveX = 0;
                        double moveY = 0;

                        if (!movedX && isVerticallyIn) {
                            double x2 = x + w;
                            double eX2 = eX + eW;

                            if (Math.abs(eX - x) < r) moveX = eX - x;
                            else if (Math.abs(eX2 - x2) <= r) moveX = eX2 - x2;
                            else if (Math.abs(eX2 - x) <= r) moveX = eX2 - x;
                            else if (Math.abs(eX - x2) <= r) moveX = eX - x2;
                        }

                        if (!movedY && isHorizontallyIn) {
                            double y2 = y + h;
                            double eY2 = eY + eH;

                            if (Math.abs(eY - y) <= r) moveY = eY - y;
                            else if (Math.abs(eY2 - y2) <= r) moveY = eY2 - y2;
                            else if (Math.abs(eY2 - y) <= r) moveY = eY2 - y;
                            else if (Math.abs(eY - y2) <= r) moveY = eY - y2;
                        }

                        if (moveX != 0 || moveY != 0) {
                            for (HudElement e : selectedElements) e.box.addPos(moveX, moveY);

                            if (moveX != 0) movedX = true;
                            if (moveY != 0) movedY = true;
                        }

                        if (movedX && movedY) break;
                    }
                }
            }

            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        private boolean isPointBetween(double start, double size, double point) {
            return point >= start && point <= start + size;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (dragging) {
                dragging = false;
                if (selectedElements.size() <= 1) selectedElements.clear();
                Input.setCursorStyle(CursorStyle.Default);
                return true;
            }

            if (selecting) {
                selecting = false;
                return true;
            }

//            if (scaling) {
//                scaling = false;
//                selectedElements.clear();
//                Input.setCursorStyle(CursorStyle.Default);
//                return true;
//            }

            return false;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            double s = mc.getWindow().getScaleFactor();

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

            for (HudElement element : selectedElements) {
                renderElement(element);
            }

            if (!dragging) {
                hoveredElement = null;

                for (HudElement element : hud.activeElements) {
                    if (element.box.intersects(mouseX, mouseY)) {
                        if (!selectedElements.contains(element)) renderElement(element);
                        hoveredElement = element;

                        break;
                    }
                }

                if (selecting) {
                    renderBox(selectStartX, selectStartY, mouseX - selectStartX, mouseY - selectStartY);
                }
            }

            if (!selecting && !dragging && hud.guides.get() == HUD.Guides.Always) {
                renderGuides(mouseX, mouseY);
            }
            else if (!selecting && dragging && hoveredElement != null && selectedElements.size() == 1 && hud.guides.get() != HUD.Guides.None) {
                double x, y;

                switch (hoveredElement.box.boxAnchorX) {
                    case Center: x = hoveredElement.box.getX() + (hoveredElement.box.width / 2); break;
                    case Right:  x = hoveredElement.box.getX() + hoveredElement.box.width; break;
                    default:     x = hoveredElement.box.getX(); break;
                }

                switch (hoveredElement.box.boxAnchorY) {
                    case Center:    y = hoveredElement.box.getY() + (hoveredElement.box.height / 2); break;
                    case Bottom:    y = hoveredElement.box.getY() + hoveredElement.box.height; break;
                    default:        y = hoveredElement.box.getY(); break;
                }

                renderGuides(x, y);
            }

            Renderer.NORMAL.end();
            Utils.scaledProjection();
        }

        private void renderElement(HudElement module) {
            renderBox(module.box.getX(), module.box.getY(), module.box.width, module.box.height);
        }

        private void renderBox(double x, double y, double w, double h) {
            Renderer.NORMAL.quad(x, y, w, h, BG_COLOR);
            Renderer.NORMAL.quad(x - 1, y - 1, w + 2, 1, OL_COLOR);
            Renderer.NORMAL.quad(x - 1, y + h - 1, w + 2, 1, OL_COLOR);
            Renderer.NORMAL.quad(x - 1, y, 1, h, OL_COLOR);
            Renderer.NORMAL.quad(x + w, y, 1, h, OL_COLOR);
        }

        private void renderGuides(double mouseX, double mouseY) {
            Renderer.NORMAL.quad(mouseX - 1, 0, 2, Utils.getWindowHeight(), OL_COLOR);
            Renderer.NORMAL.quad(0, mouseY - 1, Utils.getWindowWidth(), 2, OL_COLOR);
        }
    }

    public static class HudElementScreen extends WindowScreen {
        private final HUD hud;
        private final HudElement element;
        private WContainer settings;

        public HudElementScreen(GuiTheme theme, HudElement element) {
            super(theme, element.title);
            this.element = element;
            this.hud = HUD.get();

            // Description
            add(theme.label(element.description, getWindowWidth() / 2.0));

            // Settings
            if (element.settings.sizeGroups() > 0) {
                settings = add(theme.verticalList()).expandX().widget();
                settings.add(theme.settings(element.settings)).expandX();

                add(theme.horizontalSeparator()).expandX();
            }

            // Bottom
            WHorizontalList bottomList = add(theme.horizontalList()).expandX().widget();

            // Remove
            WMinus remove = bottomList.add(theme.minus()).widget();
            remove.action = () -> {
                hud.activeElements.remove(element);
                this.onClose();
            };

            // Reset
            WButton reset = bottomList.add(theme.button(GuiRenderer.RESET)).expandCellX().right().widget();
            reset.action = () -> element.settings.forEach(group -> group.forEach(Setting::reset));
        }

        @Override
        public void tick() {
            super.tick();

            if (settings == null) return;

            element.settings.tick(settings, theme);
        }

        @Override
        protected void onRenderBefore(float delta) {
            HUD.get().onRender(Render2DEvent.get(0, 0, delta));
        }
    }


    public static class HudElementSelectorScreen extends WindowScreen {
        private final HUD hud;

        private final WVerticalList list;

        private WSection defaultElements, custom;

        private final double clickX, clickY;

        public HudElementSelectorScreen(GuiTheme theme, double clickX, double clickY) {
            super(theme, "Add Element");

            hud = HUD.get();
            this.clickX = clickX;
            this.clickY = clickY;

            list = super.add(theme.verticalList()).expandX().minWidth(400).widget();

            defaultElements = add(theme.section("Default", defaultElements == null || defaultElements.isExpanded())).expandX().widget();
            custom = add(theme.section("Custom", custom == null || custom.isExpanded())).expandX().widget();

            initWidgets();
        }

        @Override
        public <W extends WWidget> Cell<W> add(W widget) {
            return list.add(widget);
        }

        private void initWidgets() {
            hud.elementClasses.forEach((name, klass) -> {
                switch (klass.getAnnotation(ElementRegister.class).category()) {
                    case Custom:    addElement(custom, Utils.nameToTitle(name), createItemElement()); break;
                    case Default:   addElement(defaultElements, Utils.nameToTitle(name), addElementToHud(klass)); break;
                }
            });
        }

        // Default elements
        private void addElement(WSection category, String title, Runnable action) {
            WHorizontalList elementList = category.add(theme.horizontalList()).expandX().widget();

            // Title
            elementList.add(theme.label(title)).expandX();

            // Add
            WPlus add = elementList.add(theme.plus()).expandCellX().right().widget();
            add.action = action;
        }

        // Default + button action
        private Runnable addElementToHud(Class<? extends HudElement> elementClass) {
            return () -> {
                try {
                    HudElement element = elementClass.getDeclaredConstructor().newInstance();
                    element.box.setPos(clickX, clickY);
                    hud.add(element);
                    this.onClose();
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ignored) {}
            };
        }

        private Runnable createItemElement() {
            return () -> {
                this.onClose();
                mc.openScreen(new ItemSelectorScreen(theme, clickX, clickY));
            };
        }

        public static class ItemSelectorScreen extends WindowScreen {
            private WTable table;

            private final WTextBox filter;
            private String filterText = "";

            private final double clickX, clickY;

            public ItemSelectorScreen(GuiTheme theme, double clickX, double clickY) {
                super(theme, "Select Item");

                this.clickX = clickX;
                this.clickY = clickY;

                filter = add(theme.textBox("")).minWidth(400).expandX().widget();
                filter.setFocused(true);
                filter.action = () -> {
                    filterText = filter.get().trim();

                    table.clear();
                    initWidgets();
                };

                table = add(theme.table()).expandX().widget();

                initWidgets();
            }

            private void initWidgets() {
                for (Item item : Registry.ITEM) {
                    if (item == Items.AIR) continue;

                    WItemWithLabel itemWithLabel = theme.itemWithLabel(item.getDefaultStack(), Names.get(item));
                    if (!filterText.isEmpty()) {
                        if (!StringUtils.containsIgnoreCase(itemWithLabel.getLabelText(), filterText)) continue;
                    }
                    table.add(itemWithLabel);

                    WButton select = table.add(theme.button("Select")).expandCellX().right().widget();
                    select.action = () -> {
                        CustomItemHud element = new CustomItemHud(item);
                        element.box.setPos(clickX, clickY);
                        HUD.get().add(element);
                        this.onClose();
                        onClose();
                    };

                    table.row();
                }
            }
        }
    }
}
