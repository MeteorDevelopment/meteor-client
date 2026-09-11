package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class EmberModuleScreen extends WidgetScreen {
    private static final Color PANEL_BG = new Color(12, 12, 14, 255);
    private static final Color HEADER_BG = new Color(16, 16, 20, 255);
    private static final Color SECTION_BG = new Color(8, 8, 10, 255);
    private static final Color ACCENT = new Color(255, 107, 53, 255);
    private static final Color TEXT_WHITE = new Color(255, 255, 255, 255);
    private static final Color TEXT_DIM = new Color(120, 120, 135, 255);
    private static final Color TOGGLE_OFF = new Color(42, 42, 50, 255);
    private static final Color SLIDER_BG = new Color(32, 32, 40, 255);
    private static final Color INPUT_BG = new Color(24, 24, 30, 255);
    private static final Color ROW_HOVER = new Color(255, 255, 255, 10);

    private final Module module;
    private final List<SettingElement> elements = new ArrayList<>();
    private double px, py, pw, ph;
    private int scroll = 0;
    private double mx, my;
    private int hover = -1;
    private float openAnim = 0;

    public EmberModuleScreen(GuiTheme theme, Module module) {
        super(theme, module.title);
        this.module = module;
        buildElements();
    }

    private void buildElements() {
        elements.clear();
        elements.add(new SettingElement("Active", "toggle", null));

        for (SettingGroup group : module.settings) {
            elements.add(new SettingElement(group.name, "section", null));
            for (Setting<?> s : group) {
                if (!s.isVisible()) continue;
                Object v = s.get();
                String t = "unknown";
                if (v instanceof Boolean) t = "toggle";
                else if (v instanceof Integer || v instanceof Double) t = "slider";
                else if (v instanceof Enum) t = "dropdown";
                else if (v instanceof String) t = "text";
                else if (v instanceof SettingColor) t = "color";
                elements.add(new SettingElement(s.title, t, s));
            }
        }
    }

    @Override
    public void initWidgets() {}

    @Override
    protected void onRenderBefore(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        double s = mc.getWindow().getGuiScale();
        mx = mouseX * s;
        my = mouseY * s;

        openAnim = Math.min(1f, openAnim + delta * 0.12f);
        float ease = 1f - (float) Math.pow(1 - openAnim, 3);

        pw = Math.min(420, getWindowWidth() - 80);
        ph = Math.min(500, getWindowHeight() - 60);
        px = (getWindowWidth() - pw) / 2;
        py = (getWindowHeight() - ph) / 2;

        GuiRenderer r = new GuiRenderer();
        r.theme = theme;
        r.begin(graphics);

        // Overlay
        r.quad(0, 0, getWindowWidth(), getWindowHeight(), new Color(0, 0, 0, (int) (180 * ease)));

        // Shadow
        for (int i = 16; i > 0; i -= 3) {
            float a = (float) i / 16f;
            r.quad(px - i, py - i, pw + i * 2, ph + i * 2, new Color(0, 0, 0, (int) (45 * a * ease)));
        }

        // Panel
        double rad = 12;
        r.roundedRect(px, py, pw, ph, rad, PANEL_BG);

        // Header
        double hH = 50;
        r.roundedRect(px, py, pw, hH, rad, HEADER_BG);
        r.quad(px + rad, py + hH - rad, pw - rad * 2, rad, HEADER_BG);
        r.quad(px, py + hH - 2, pw, 2, ACCENT);

        // Glow under accent
        Color g1 = new Color(ACCENT.r, ACCENT.g, ACCENT.b, (int) (35 * ease));
        Color g2 = new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0);
        r.quad(px, py + hH, pw, 14, g1, g1, g2, g2);

        // Close button
        double closeX = px + pw - 44;
        double closeY = py + 10;
        boolean hoverClose = mx >= closeX && mx < closeX + 32 && my >= closeY && my < closeY + 30;
        if (hoverClose) {
            r.roundedRect(closeX, closeY, 32, 30, 6, new Color(255, 60, 60, 50));
        }

        // Settings rows
        double rowH = 36;
        double secH = 28;
        double contentX = px + 14;
        double contentW = pw - 28;
        double contentY = py + hH + 12 - scroll;
        double clipTop = py + hH;
        double clipBot = py + ph - 8;
        hover = -1;

        double y = contentY;
        for (int i = 0; i < elements.size(); i++) {
            SettingElement e = elements.get(i);

            if (e.type.equals("section")) {
                if (y + secH > clipTop && y < clipBot) {
                    r.roundedRect(contentX, y, contentW, secH, 6, SECTION_BG);
                }
                y += secH + 6;
                continue;
            }

            if (y + rowH > clipTop && y < clipBot) {
                boolean h = mx >= contentX && mx < contentX + contentW && my >= y && my < y + rowH;
                if (h) {
                    hover = i;
                    r.roundedRect(contentX, y, contentW, rowH, 6, ROW_HOVER);
                }

                double ctrlY = y + (rowH - 20) / 2;

                if (e.type.equals("toggle")) {
                    boolean val = e.setting == null ? module.isActive() : (Boolean) e.setting.get();
                    double tW = 38, tH = 20;
                    double tX = contentX + contentW - tW - 10;

                    Color track = val ? ACCENT : TOGGLE_OFF;
                    r.roundedRect(tX, ctrlY, tW, tH, tH / 2, track);

                    double knob = tH - 4;
                    double kX = val ? tX + tW - knob - 2 : tX + 2;
                    r.quad(kX, ctrlY + 2, knob, knob, GuiRenderer.CIRCLE, TEXT_WHITE);

                } else if (e.type.equals("slider")) {
                    double sW = 100, sH = 4;
                    double sX = contentX + contentW - sW - 48;
                    double sY = y + (rowH - sH) / 2;

                    r.roundedRect(sX, sY, sW, sH, 2, SLIDER_BG);

                    double pct = 0.5;
                    if (e.setting instanceof IntSetting is) {
                        pct = (double) (is.get() - is.min) / Math.max(1, is.max - is.min);
                    } else if (e.setting instanceof DoubleSetting ds) {
                        pct = (ds.get() - ds.min) / Math.max(0.001, ds.max - ds.min);
                    }
                    pct = Mth.clamp(pct, 0, 1);
                    if (sW * pct > 4) {
                        r.roundedRect(sX, sY, sW * pct, sH, 2, ACCENT);
                    }

                    double kX = sX + sW * pct - 5;
                    r.quad(Math.max(sX, kX), sY - 5, 10, sH + 10, GuiRenderer.CIRCLE, TEXT_WHITE);

                } else if (e.type.equals("dropdown") || e.type.equals("color") || e.type.equals("text")) {
                    r.roundedRect(contentX + contentW - 116, ctrlY, 106, 22, 6, INPUT_BG);
                }
            }

            y += rowH + 2;
        }

        r.end();

        // Header title text
        theme.textRenderer().begin(graphics, theme.scale(1.3));
        theme.textRenderer().render(module.title, px + 16, py + (hH - theme.textHeight() * 1.3) / 2, TEXT_WHITE, false);
        theme.textRenderer().end();

        // Close X text
        theme.textRenderer().begin(graphics, theme.scale(1.1));
        theme.textRenderer().render("X", closeX + 10, closeY + 6,
            hoverClose ? new Color(255, 120, 120) : TEXT_DIM, false);
        theme.textRenderer().end();

        // Setting labels
        theme.textRenderer().begin(graphics, theme.scale(0.95));
        y = contentY;
        for (int i = 0; i < elements.size(); i++) {
            SettingElement e = elements.get(i);

            if (e.type.equals("section")) {
                if (y + secH > clipTop && y < clipBot) {
                    theme.textRenderer().render(e.name, contentX + 10, y + (secH - theme.textHeight() * 0.95) / 2, TEXT_DIM, false);
                }
                y += secH + 6;
                continue;
            }

            if (y + rowH > clipTop && y < clipBot) {
                theme.textRenderer().render(e.name, contentX + 12, y + (rowH - theme.textHeight() * 0.95) / 2, TEXT_WHITE, false);

                if (e.type.equals("slider") && e.setting != null) {
                    String val = "";
                    if (e.setting instanceof IntSetting is) val = String.valueOf(is.get());
                    else if (e.setting instanceof DoubleSetting ds) val = String.format("%.1f", ds.get());
                    double tw = theme.textWidth(val) * 0.95;
                    theme.textRenderer().render(val, contentX + contentW - 44 - tw, y + (rowH - theme.textHeight() * 0.95) / 2, ACCENT, false);
                } else if (e.type.equals("dropdown") && e.setting != null) {
                    String val = e.setting.get().toString();
                    if (val.length() > 12) val = val.substring(0, 10) + "..";
                    theme.textRenderer().render(val, contentX + contentW - 112, y + (rowH - theme.textHeight() * 0.95) / 2 + 2, ACCENT, false);
                }
            }

            y += rowH + 2;
        }
        theme.textRenderer().end();
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        double s = mc.getWindow().getGuiScale();
        double clickX = click.x() * s;
        double clickY = click.y() * s;

        double closeX = px + pw - 44;
        double closeY = py + 10;
        if (clickX >= closeX && clickX < closeX + 32 && clickY >= closeY && clickY < closeY + 30) {
            onClose();
            return true;
        }

        if (hover >= 0 && hover < elements.size()) {
            SettingElement e = elements.get(hover);
            if (e.type.equals("toggle")) {
                if (e.setting == null) module.toggle();
                else if (e.setting instanceof BoolSetting bs) bs.set(!bs.get());
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        scroll -= (int) (v * 26);
        scroll = Math.max(0, scroll);
        int max = Math.max(0, elements.size() * 40 - (int) ph + 80);
        scroll = Math.min(scroll, max);
        return true;
    }

    @Override
    public void onClose() {
        mc.gui.setScreen(parent);
    }

    private static class SettingElement {
        String name, type;
        Setting<?> setting;
        SettingElement(String n, String t, Setting<?> s) { name = n; type = t; setting = s; }
    }
}
