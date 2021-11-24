/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WQuad;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ColorSettingScreen extends WindowScreen {
    private static final Color[] HUE_COLORS = { new Color(255, 0, 0), new Color(255, 255, 0), new Color(0, 255, 0), new Color(0, 255, 255), new Color(0, 0, 255), new Color(255, 0, 255), new Color(255, 0, 0) };
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color BLACK = new Color(0, 0, 0);

    public Runnable action;

    private final Setting<SettingColor> setting;

    private WQuad displayQuad;

    private WBrightnessQuad brightnessQuad;
    private WHueQuad hueQuad;

    private WIntEdit rItb, gItb, bItb, aItb;
    private WCheckbox rainbow;

    public ColorSettingScreen(GuiTheme theme, Setting<SettingColor> setting) {
        super(theme, "Select Color");

        this.setting = setting;
    }

    @Override
    public boolean toClipboard() {
        String color = setting.get().toString().replace(" ", ",");
        mc.keyboard.setClipboard(color);
        return mc.keyboard.getClipboard().equals(color);
    }

    @Override
    public boolean fromClipboard() {
        String clipboard = mc.keyboard.getClipboard().trim();
        SettingColor parsed;

        if ((parsed = parseRGBA(clipboard)) != null) {
            setting.get().rainbow(false);
            setting.set(parsed);
            setting.get().validate();
            return true;
        }

        if ((parsed = parseHex(clipboard)) != null) {
            setting.get().rainbow(false);
            setting.set(parsed);
            setting.get().validate();
            return true;
        }

        return false;
    }

    private SettingColor parseRGBA(String string) {
        String[] rgba = string.replaceAll("[^0-9|,]", "").split(",");
        if (rgba.length < 3 || rgba.length > 4) return null;

        SettingColor color;
        try {
            color = new SettingColor(Integer.parseInt(rgba[0]), Integer.parseInt(rgba[1]), Integer.parseInt(rgba[2]));
            if (rgba.length == 4) color.a = Integer.parseInt(rgba[3]);
        }
        catch (NumberFormatException e) {
            return null;
        }

        return color;
    }

    private SettingColor parseHex(String string) {
        if (!string.startsWith("#")) return null;
        String hex = string.toLowerCase().replaceAll("[^0-9a-f]", "");
        if (hex.length() != 6 && hex.length() != 8) return null;

        SettingColor color;
        try {
            color = new SettingColor(Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16), Integer.parseInt(hex.substring(4, 6), 16));
            if (hex.length() == 8) color.a = Integer.parseInt(hex.substring(6, 8), 16);
        }
        catch (NumberFormatException e) {
            return null;
        }

        return color;
    }

    @Override
    public void initWidgets() {
        // Top
        displayQuad = add(theme.quad(setting.get())).expandX().widget();

        brightnessQuad = add(new WBrightnessQuad()).expandX().widget();

        hueQuad = add(new WHueQuad()).expandX().widget();

        // RGBA
        WTable rgbaTable = add(theme.table()).expandX().widget();

        rgbaTable.add(theme.label("R:"));
        rItb = rgbaTable.add(theme.intEdit(setting.get().r, 0, 255, 0, 255, false)).expandX().widget();
        rItb.action = this::rgbaChanged;
        rgbaTable.row();

        rgbaTable.add(theme.label("G:"));
        gItb = rgbaTable.add(theme.intEdit(setting.get().g, 0, 255, 0, 255, false)).expandX().widget();
        gItb.action = this::rgbaChanged;
        rgbaTable.row();

        rgbaTable.add(theme.label("B:"));
        bItb = rgbaTable.add(theme.intEdit(setting.get().b, 0, 255, 0, 255, false)).expandX().widget();
        bItb.action = this::rgbaChanged;
        rgbaTable.row();

        rgbaTable.add(theme.label("A:"));
        aItb = rgbaTable.add(theme.intEdit(setting.get().a, 0, 255, 0, 255, false)).expandX().widget();
        aItb.action = this::rgbaChanged;

        // Rainbow
        WHorizontalList rainbowList = add(theme.horizontalList()).expandX().widget();
        rainbowList.add(theme.label("Rainbow: "));
        rainbow = theme.checkbox(setting.get().rainbow);
        rainbow.action = () -> {
            setting.get().rainbow = rainbow.checked;
            setting.onChanged();
        };
        rainbowList.add(rainbow).expandCellX().right();

        // Bottom
        WHorizontalList bottomList = add(theme.horizontalList()).expandX().widget();

        WButton backButton = bottomList.add(theme.button("Back")).expandX().widget();
        backButton.action = this::onClose;

        WButton resetButton = bottomList.add(theme.button(GuiRenderer.RESET)).widget();
        resetButton.action = () -> {
            setting.reset();
            setFromSetting();
            callAction();
        };

        hueQuad.calculateFromSetting(false);
        brightnessQuad.calculateFromColor(setting.get(), false);
    }

    private void setFromSetting() {
        SettingColor c = setting.get();

        if (c.r != rItb.get()) rItb.set(c.r);
        if (c.g != gItb.get()) gItb.set(c.g);
        if (c.b != bItb.get()) bItb.set(c.b);
        if (c.a != aItb.get()) aItb.set(c.a);
        rainbow.checked = c.rainbow;

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
        if (setting.get().rainbow) setFromSetting();
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

        setting.onChanged();
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
                case 0 -> {
                    r = brightnessQuad.value;
                    g = t;
                    b = p;
                }
                case 1 -> {
                    r = q;
                    g = brightnessQuad.value;
                    b = p;
                }
                case 2 -> {
                    r = p;
                    g = brightnessQuad.value;
                    b = t;
                }
                case 3 -> {
                    r = p;
                    g = q;
                    b = brightnessQuad.value;
                }
                case 4 -> {
                    r = t;
                    g = p;
                    b = brightnessQuad.value;
                }
                default -> {
                    r = brightnessQuad.value;
                    g = p;
                    b = q;
                }
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
        setting.onChanged();
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
        protected void onCalculateSize() {
            double s = theme.scale(75);

            width = s;
            height = s;

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
        public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) {
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
        public boolean onMouseReleased(double mouseX, double mouseY, int button) {
            if (dragging) {
                dragging = false;
            }

            return false;
        }

        @Override
        public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
            if (dragging) {
                if (mouseX >= this.x && mouseX <= this.x + width) {
                    handleX += mouseX - lastMouseX;
                } else {
                    if (handleX > 0 && mouseX < this.x) handleX = 0;
                    else if (handleX < width && mouseX > this.x + width) handleX = width;
                }

                if (mouseY >= this.y && mouseY <= this.y + height) {
                    handleY += mouseY - lastMouseY;
                } else {
                    if (handleY > 0 && mouseY < this.y) handleY = 0;
                    else if (handleY < height && mouseY > this.y + height) handleY = height;
                }

                handleMoved();
            }

            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
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

            renderer.quad(x, y, width, height, WHITE, hueQuad.color, BLACK, BLACK);

            double s = theme.scale(2);
            renderer.quad(x + handleX - s / 2, y + handleY - s / 2, s, s, WHITE);
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
        protected void onCalculateSize() {
            width = theme.scale(75);
            height = theme.scale(10);
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

            switch (i) {
                case 0 -> {
                    r = 1;
                    g = t;
                    b = p;
                }
                case 1 -> {
                    r = q;
                    g = 1;
                    b = p;
                }
                case 2 -> {
                    r = p;
                    g = 1;
                    b = t;
                }
                case 3 -> {
                    r = p;
                    g = q;
                    b = 1;
                }
                case 4 -> {
                    r = t;
                    g = p;
                    b = 1;
                }
                default -> {
                    r = 1;
                    g = p;
                    b = q;
                }
            }

            color.r = (int) (r * 255);
            color.g = (int) (g * 255);
            color.b = (int) (b * 255);
            color.validate();
        }

        @Override
        public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) {
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
        public boolean onMouseReleased(double mouseX, double mouseY, int button) {
            if (dragging) {
                dragging = false;
            }

            return mouseOver;
        }

        @Override
        public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
            if (dragging) {
                if (mouseX >= this.x && mouseX <= this.x + width) {
                    handleX += mouseX - lastMouseX;
                    handleX = Utils.clamp(handleX, 0, width);
                } else {
                    if (handleX > 0 && mouseX < this.x) handleX = 0;
                    else if (handleX < width && mouseX > this.x + width) handleX = width;
                }

                calculateHueAngleFromHandleX();
                hsvChanged();
            }

            this.lastMouseX = mouseX;
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
                renderer.quad(sectionX, y, sectionWidth, height, HUE_COLORS[i], HUE_COLORS[i + 1], HUE_COLORS[i + 1], HUE_COLORS[i]);
                sectionX += sectionWidth;
            }

            double s = theme.scale(2);
            renderer.quad(x + handleX - s / 2, y, s, height, WHITE);
        }
    }
}
