/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.renderer.Region;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;

public class ColorSettingScreen extends WindowScreen {
    private static final Color[] HUE_COLORS = { new Color(255, 0, 0), new Color(255, 255, 0), new Color(0, 255, 0), new Color(0, 255, 255), new Color(0, 0, 255), new Color(255, 0, 255), new Color(255, 0, 0) };
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color BLACK = new Color(0, 0, 0);

    public Runnable action;

    private final Setting<SettingColor> setting;

    private final WQuad displayQuad;

    private final WBrightnessQuad brightnessQuad;
    private final WHueQuad hueQuad;

    private final WIntEdit rItb, gItb, bItb, aItb;
    private final WDoubleEdit rainbowSpeed;

    public ColorSettingScreen(Setting<SettingColor> setting) {
        super("Select Color", true);
        this.setting = setting;

        displayQuad = add(new WQuad(setting.get())).fillX().expandX().getWidget();
        row();

        brightnessQuad = add(new WBrightnessQuad()).fillX().expandX().getWidget();
        row();

        hueQuad = add(new WHueQuad()).fillX().expandX().getWidget();
        row();

        WTable rgbaTable = add(new WTable()).fillX().expandX().getWidget();
        row();

        rgbaTable.add(new WLabel("R:"));
        rItb = rgbaTable.add(new WIntEdit(setting.get().r, 0, 255)).getWidget();
        rItb.action = this::rgbaChanged;
        rgbaTable.row();

        rgbaTable.add(new WLabel("G:"));
        gItb = rgbaTable.add(new WIntEdit(setting.get().g, 0, 255)).getWidget();
        gItb.action = this::rgbaChanged;
        rgbaTable.row();

        rgbaTable.add(new WLabel("B:"));
        bItb = rgbaTable.add(new WIntEdit(setting.get().b, 0, 255)).getWidget();
        bItb.action = this::rgbaChanged;
        rgbaTable.row();

        rgbaTable.add(new WLabel("A:"));
        aItb = rgbaTable.add(new WIntEdit(setting.get().a, 0, 255)).getWidget();
        aItb.action = this::rgbaChanged;

        WTable rainbowTable = add(new WTable()).fillX().expandX().getWidget();
        row();
        rainbowTable.add(new WLabel("Rainbow: "));
        rainbowSpeed = rainbowTable.add(new WDoubleEdit(setting.get().rainbowSpeed, 0, 0.025, 4, false, 75)).fillX().expandX().getWidget();
        rainbowSpeed.action = () -> {
            setting.get().rainbowSpeed = rainbowSpeed.get();
            setting.changed();
        };

        WTable bottomTable = add(new WTable()).fillX().expandX().getWidget();

        WButton backButton = bottomTable.add(new WButton("Back")).fillX().expandX().getWidget();
        backButton.action = this::onClose;

        WButton resetButton = bottomTable.add(new WButton(WButton.ButtonRegion.Reset)).getWidget();
        resetButton.action = () -> {
            setting.reset();
            setFromSetting();
            callAction();
        };

        hueQuad.calculateFromSetting(false);
        brightnessQuad.calculateFromColor(setting.get(), false);
    }

    private void setFromSetting() {
        rItb.set(setting.get().r);
        gItb.set(setting.get().g);
        bItb.set(setting.get().b);
        aItb.set(setting.get().a);
        rainbowSpeed.set(setting.get().rainbowSpeed);

        displayQuad.color.set(setting.get());
        hueQuad.calculateFromSetting(true);
        brightnessQuad.calculateFromColor(setting.get(), true);
    }

    private void callAction() {
        if (action != null) action.run();
    }

    @Override
    public void tick() {
        super.tick();

        if (setting.get().rainbowSpeed > 0) setFromSetting();
    }

    private void rgbaChanged() {
        Color c = setting.get();

        c.r = rItb.get();
        c.g = gItb.get();
        c.b = bItb.get();
        c.a = aItb.get();

        c.validate();

        if (c.r != rItb.get()) rItb.set(c.r);
        if (c.g != gItb.get()) gItb.set(c.g);
        if (c.b != bItb.get()) bItb.set(c.b);
        if (c.a != aItb.get()) aItb.set(c.a);

        displayQuad.color.set(c);
        hueQuad.calculateFromSetting(true);
        brightnessQuad.calculateFromColor(setting.get(), true);

        setting.changed();
        callAction();
    }

    private void hsvChanged() {
        double hh, p, q, t, ff;
        int i;

        double r = 0;
        double g = 0;
        double b = 0;
        boolean calculated = false;

        if(brightnessQuad.saturation <= 0.0) {
            r = brightnessQuad.value;
            g = brightnessQuad.value;
            b = brightnessQuad.value;
            calculated = true;
        }

        if (!calculated) {
            hh = hueQuad.hueAngle;
            if (hh >= 360.0) hh = 0.0;
            hh /= 60.0;
            i = (int) hh;
            ff = hh - i;
            p = brightnessQuad.value * (1.0 - brightnessQuad.saturation);
            q = brightnessQuad.value * (1.0 - (brightnessQuad.saturation * ff));
            t = brightnessQuad.value * (1.0 - (brightnessQuad.saturation * (1.0 - ff)));

            switch (i) {
                case 0:
                    r = brightnessQuad.value;
                    g = t;
                    b = p;
                    break;
                case 1:
                    r = q;
                    g = brightnessQuad.value;
                    b = p;
                    break;
                case 2:
                    r = p;
                    g = brightnessQuad.value;
                    b = t;
                    break;

                case 3:
                    r = p;
                    g = q;
                    b = brightnessQuad.value;
                    break;
                case 4:
                    r = t;
                    g = p;
                    b = brightnessQuad.value;
                    break;
                case 5:
                default:
                    r = brightnessQuad.value;
                    g = p;
                    b = q;
                    break;
            }
        }

        Color c = setting.get();

        c.r = (int) (r * 255);
        c.g = (int) (g * 255);
        c.b = (int) (b * 255);
        c.validate();

        rItb.set(c.r);
        gItb.set(c.g);
        bItb.set(c.b);

        displayQuad.color.set(c);
        setting.changed();
        callAction();
    }

    private class WBrightnessQuad extends WWidget {
        double saturation, value;

        double handleX, handleY;

        boolean dragging;
        double lastMouseX, lastMouseY;

        boolean calculateHandlePosOnLayout;

        double fixedHeight = -1;

        @Override
        protected void onCalculateSize(GuiRenderer renderer) {
            width = 100 * GuiConfig.get().guiScale;
            height = 100 * GuiConfig.get().guiScale;

            if (fixedHeight != -1) {
                height = fixedHeight;
                fixedHeight = -1;
            }
        }

        void calculateFromColor(Color c, boolean calculateNow) {
            double min = Math.min(Math.min(c.r, c.g), c.b);
            double max = Math.max(Math.max(c.r, c.g), c.b);
            double delta = max - min;

            value = max / 255;
            if(delta == 0){
                saturation = 0;
            }else {
                saturation = delta / max;
            }

            if (calculateNow) {
                handleX = saturation * width;
                handleY = (1 - value) * height;
            } else {
                calculateHandlePosOnLayout = true;
            }
        }

        @Override
        protected void onCalculateWidgetPositions() {
            if (calculateHandlePosOnLayout) {
                handleX = saturation * width;
                handleY = (1 - value) * height;

                calculateHandlePosOnLayout = false;
            }

            super.onCalculateWidgetPositions();
        }

        @Override
        protected boolean onMouseClicked(boolean used, int button) {
            if (used) return false;

            if (mouseOver) {
                dragging = true;

                handleX = lastMouseX - x;
                handleY = lastMouseY - y;
                handleMoved();

                return true;
            }

            return false;
        }

        @Override
        protected boolean onMouseReleased(boolean used, int button) {
            if (dragging) {
                dragging = false;
            }

            return mouseOver && !used;
        }

        @Override
        protected void onMouseMoved(double x, double y) {
            if (dragging) {
                if (x >= this.x && x <= this.x + width) {
                    handleX += x - lastMouseX;
                } else {
                    if (handleX > 0 && x < this.x) handleX = 0;
                    else if (handleX < width && x > this.x + width) handleX = width;
                }

                if (y >= this.y && y <= this.y + height) {
                    handleY += y - lastMouseY;
                } else {
                    if (handleY > 0 && y < this.y) handleY = 0;
                    else if (handleY < height && y > this.y + height) handleY = height;
                }

                handleMoved();
            }

            lastMouseX = x;
            lastMouseY = y;
        }

        void handleMoved() {
            double handleXPercentage = handleX / width;
            double handleYPercentage = handleY / height;

            saturation = handleXPercentage;
            value = 1 - handleYPercentage;

            hsvChanged();
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (height != width) {
                fixedHeight = width;
                invalidate();
            }

            hueQuad.calculateColor();

            renderer.quad(Region.FULL, x, y, width, height, WHITE, hueQuad.color, BLACK, BLACK);

            Color color = GuiConfig.get().colorEditHandle;
            if (dragging) color = GuiConfig.get().colorEditHandlePressed;
            else if (mouseX >= x + handleX - 1 && mouseX <= x + handleX + 1 && mouseY >= y + handleY - 1 && mouseY <= y + handleY + 1) color = GuiConfig.get().colorEditHandleHovered;

            renderer.quad(Region.FULL, x + handleX - 1, y + handleY - 1, 2, 2, color);
        }
    }

    private class WHueQuad extends WWidget {
        private double hueAngle;
        private double handleX;

        private final Color color = new Color();

        private boolean dragging;
        private double lastMouseX;

        private boolean calculateHandleXOnLayout;

        @Override
        protected void onCalculateSize(GuiRenderer renderer) {
            width = 100 * GuiConfig.get().guiScale;
            height = 10 * GuiConfig.get().guiScale;
        }

        void calculateFromSetting(boolean calculateNow) {
            Color c = setting.get();
            boolean calculated = false;

            double min, max, delta;

            min = Math.min(c.r, c.g);
            min = min  < c.b ? min  : c.b;

            max = Math.max(c.r, c.g);
            max = max  > c.b ? max  : c.b;

            delta = max - min;
            if (delta < 0.00001) {
                hueAngle = 0;
                calculated = true;
            }

            if (!calculated) {
                if (max <= 0.0) { // NOTE: if Max is == 0, this divide would cause a crash
                    // if max is 0, then r = g = b = 0
                    // s = 0, h is undefined
                    hueAngle = 0;
                    calculated = true;
                }

                if (!calculated) {
                    if (c.r >= max) hueAngle = (c.g - c.b) / delta; // between yellow & magenta
                    else if (c.g >= max) hueAngle = 2.0 + (c.b - c.r) / delta; // between cyan & yellow
                    else hueAngle = 4.0 + (c.r - c.g) / delta; // between magenta & cyan

                    hueAngle *= 60.0; // degrees

                    if (hueAngle < 0.0) hueAngle += 360.0;
                }
            }

            if (calculateNow) {
                double huePercentage = hueAngle / 360;
                handleX = huePercentage * width;
            } else {
                calculateHandleXOnLayout = true;
            }
        }

        @Override
        protected void onCalculateWidgetPositions() {
            if (calculateHandleXOnLayout) {
                double huePercentage = hueAngle / 360;
                handleX = huePercentage * width;

                calculateHandleXOnLayout = false;
            }

            super.onCalculateWidgetPositions();
        }

        void calculateColor() {
            double hh, p, q, t, ff;
            int i;

            hh = hueAngle;
            if(hh >= 360.0) hh = 0.0;
            hh /= 60.0;
            i = (int) hh;
            ff = hh - i;
            p = 1 * (1.0 - 1);
            q = 1 * (1.0 - (1 * ff));
            t = 1 * (1.0 - (1 * (1.0 - ff)));

            double r;
            double g;
            double b;

            switch(i) {
                case 0:
                    r = 1;
                    g = t;
                    b = p;
                    break;
                case 1:
                    r = q;
                    g = 1;
                    b = p;
                    break;
                case 2:
                    r = p;
                    g = 1;
                    b = t;
                    break;
                case 3:
                    r = p;
                    g = q;
                    b = 1;
                    break;
                case 4:
                    r = t;
                    g = p;
                    b = 1;
                    break;
                case 5:
                default:
                    r = 1;
                    g = p;
                    b = q;
                    break;
            }

            color.r = (int) (r * 255);
            color.g = (int) (g * 255);
            color.b = (int) (b * 255);
            color.validate();
        }

        @Override
        protected boolean onMouseClicked(boolean used, int button) {
            if (used) return false;

            if (mouseOver) {
                dragging = true;

                handleX = lastMouseX - x;
                calculateHueAngleFromHandleX();
                hsvChanged();

                return true;
            }

            return false;
        }

        @Override
        protected boolean onMouseReleased(boolean used, int button) {
            if (dragging) {
                dragging = false;
            }

            return mouseOver && !used;
        }

        @Override
        protected void onMouseMoved(double x, double y) {
            if (dragging) {
                if (x >= this.x && x <= this.x + width) {
                    handleX += x - lastMouseX;
                    handleX = Utils.clamp(handleX, 0, width);
                } else {
                    if (handleX > 0 && x < this.x) handleX = 0;
                    else if (handleX < width && x > this.x + width) handleX = width;
                }

                calculateHueAngleFromHandleX();
                hsvChanged();
            }

            lastMouseX = x;
        }

        void calculateHueAngleFromHandleX() {
            double handleXPercentage = handleX / (width - 4);
            hueAngle = handleXPercentage * 360;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double sectionWidth = (width) / (HUE_COLORS.length - 1);
            double sectionX = x;

            for (int i = 0; i < HUE_COLORS.length - 1; i++) {
                renderer.quad(Region.FULL, sectionX, y, sectionWidth, height, HUE_COLORS[i], HUE_COLORS[i + 1], HUE_COLORS[i + 1], HUE_COLORS[i]);
                sectionX += sectionWidth;
            }

            Color color = GuiConfig.get().colorEditHandle;
            if (dragging) color = GuiConfig.get().colorEditHandlePressed;
            else if (mouseX >= x + handleX - 1 && mouseX <= x + handleX + 1 && mouseY >= y && mouseY <= y + height) color = GuiConfig.get().colorEditHandleHovered;

            renderer.quad(Region.FULL, x + handleX - 1, y, 2, height, color);
        }
    }
}
