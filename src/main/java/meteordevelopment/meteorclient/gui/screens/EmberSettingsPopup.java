package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.EmberAnim;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.EmberPalette;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Mth;

import java.util.*;
import java.util.function.BooleanSupplier;

import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Roomy settings editor shown over the ClickGUI. Every setting gets a full-width row with
 * its title and description on the left and a proper control on the right.
 */
public class EmberSettingsPopup {
    private static final Color DANGER = new Color(222, 86, 86, 255);

    // Refreshed from EmberPalette every frame, so the popup follows the selected theme.
    private static Color PANEL_BG, HEADER_BG, DIVIDER, CONTROL_BG, CONTROL_HOVER, TRACK, TOGGLE_OFF, TEXT, TEXT_DIM, TEXT_FAINT;

    static {
        syncPalette();
    }

    private static final double MAX_W = 480;
    private static final double HEADER_H = 64;
    private static final double FOOTER_H = 30;
    private static final double GROUP_H = 34;
    private static final double ROW_H = 46;
    private static final double SUB_H = 32;
    private static final double PAD = 20;
    private static final double CTRL_W = 200;
    private static final double RADIUS = 12;

    private static final double TITLE_SCALE = 1.05;
    private static final double LABEL_SCALE = 0.84;
    private static final double SMALL_SCALE = 0.66;

    /** What the popup edits: a module or a HUD element. */
    public record Target(String title, String description, Settings settings, BooleanSupplier active,
                         Runnable toggle, Keybind keybind, Runnable openNativeEditor) {
    }

    private enum Kind { GROUP, MODULE_BIND, BOOL, INT, DOUBLE, ENUM, STRING, KEYBIND, COLOR, CHANNEL, RAINBOW, OTHER }

    private record Row(Kind kind, String title, String description, Setting<?> setting, int channel, double height) {
    }

    private record Hit(double x, double y, double w, double h, Row row, String area) {
        boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private record TextOp(String text, double x, double y, Color color, double scale, int align, double box) {
    }

    private final GuiTheme theme;
    private final EmberAnim.Clock clock = new EmberAnim.Clock();

    private Target target;
    private float openAnim;
    private boolean closing;

    private double scroll;
    private double maxScroll;
    private double mx, my;

    private final List<Hit> hits = new ArrayList<>();
    private final List<TextOp> texts = new ArrayList<>();
    private final Map<Object, Float> anims = new HashMap<>();
    private final Set<Setting<?>> expandedColors = new HashSet<>();

    private Row dragRow;
    private Hit dragHit;
    private boolean mouseDown;

    private StringSetting editing;
    private String editBuffer = "";

    private Keybind capturing;
    private Setting<?> capturingSetting;

    public EmberSettingsPopup(GuiTheme theme) {
        this.theme = theme;
    }

    private static void syncPalette() {
        PANEL_BG = new Color(EmberPalette.panel().r, EmberPalette.panel().g, EmberPalette.panel().b, 250);
        HEADER_BG = EmberPalette.header();
        DIVIDER = EmberPalette.divider();
        CONTROL_BG = EmberPalette.control();
        CONTROL_HOVER = EmberPalette.controlHover();
        TRACK = EmberPalette.track();
        TOGGLE_OFF = EmberPalette.toggleOff();
        TEXT = EmberPalette.textBright();
        TEXT_DIM = EmberPalette.textDim();
        TEXT_FAINT = EmberPalette.textFaint();
    }

    public void open(Target target) {
        this.target = target;
        closing = false;
        openAnim = 0;
        scroll = 0;
        expandedColors.clear();
        stopEditing(false);
        capturing = null;
    }

    public void close() {
        stopEditing(true);
        capturing = null;
        dragRow = null;
        closing = true;
    }

    /** True while the popup is on screen, including its closing animation. */
    public boolean isVisible() {
        return target != null;
    }

    private Color accent() {
        return EmberPalette.accent();
    }

    private Color accentAlpha(int a) {
        Color c = accent();
        return new Color(c.r, c.g, c.b, Mth.clamp(a, 0, 255));
    }

    private static Color alpha(Color c, float f) {
        return new Color(c.r, c.g, c.b, (int) (c.a * f));
    }

    /** Real-time easing: settles in about a fifth of a second regardless of frame rate. */
    private float anim(Object key, boolean on, float dt) {
        float target = on ? 1f : 0f;
        // First sighting starts at the target, so rows don't all flash their hover state on open.
        float cur = EmberAnim.approach(anims.getOrDefault(key, target), target, dt, 0.065);
        anims.put(key, cur);
        return cur;
    }

    // --- Layout ---

    private List<Row> buildRows() {
        List<Row> rows = new ArrayList<>();

        if (target.keybind() != null) {
            rows.add(new Row(Kind.GROUP, "Module", null, null, 0, GROUP_H));
            rows.add(new Row(Kind.MODULE_BIND, "Bind", "Key that toggles this module.", null, 0, ROW_H));
        }

        for (SettingGroup group : target.settings()) {
            boolean headerAdded = false;

            for (Setting<?> setting : group) {
                if (!setting.isVisible()) continue;

                if (!headerAdded) {
                    rows.add(new Row(Kind.GROUP, group.name, null, null, 0, GROUP_H));
                    headerAdded = true;
                }

                Kind kind = kindOf(setting);
                rows.add(new Row(kind, setting.title, setting.description, setting, 0, ROW_H));

                if (kind == Kind.COLOR && expandedColors.contains(setting)) {
                    for (int channel = 0; channel < 4; channel++) {
                        rows.add(new Row(Kind.CHANNEL, "RGBA".substring(channel, channel + 1), null, setting, channel, SUB_H));
                    }
                    rows.add(new Row(Kind.RAINBOW, "Rainbow", null, setting, 0, SUB_H));
                }
            }
        }

        return rows;
    }

    private static Kind kindOf(Setting<?> setting) {
        if (setting instanceof BoolSetting) return Kind.BOOL;
        if (setting instanceof IntSetting) return Kind.INT;
        if (setting instanceof DoubleSetting) return Kind.DOUBLE;
        if (setting instanceof EnumSetting<?>) return Kind.ENUM;
        if (setting instanceof StringSetting) return Kind.STRING;
        if (setting instanceof KeybindSetting) return Kind.KEYBIND;
        if (setting instanceof ColorSetting) return Kind.COLOR;
        return Kind.OTHER;
    }

    // --- Render ---

    public void render(GuiGraphicsExtractor graphics, double mouseX, double mouseY, float delta) {
        float dt = clock.tick();
        if (target == null) return;

        syncPalette();

        mx = mouseX;
        my = mouseY;

        float step = dt / 0.18f;
        openAnim = closing ? Math.max(0f, openAnim - step) : Math.min(1f, openAnim + step);
        if (closing && openAnim <= 0f) {
            target = null;
            return;
        }

        float fade = 1f - (1f - openAnim) * (1f - openAnim) * (1f - openAnim);

        if (dragRow != null && mouseDown) applySlider(dragRow, dragHit, mx);

        List<Row> rows = buildRows();
        double contentH = 0;
        for (Row row : rows) contentH += row.height();

        double w = Math.min(MAX_W, getWindowWidth() - 40);
        double h = Math.min(getWindowHeight() - 80, HEADER_H + contentH + FOOTER_H + 8);
        double x = (getWindowWidth() - w) / 2;
        double y = (getWindowHeight() - h) / 2 + 14 * (1 - fade);

        double bodyTop = y + HEADER_H;
        double bodyBottom = y + h - FOOTER_H;
        maxScroll = Math.max(0, contentH + 8 - (bodyBottom - bodyTop));
        scroll = Mth.clamp(scroll, 0, maxScroll);

        hits.clear();
        texts.clear();

        GuiRenderer r = new GuiRenderer();
        r.theme = theme;
        r.begin(graphics);

        // Dim everything behind; its own batch so it doesn't darken the glow.
        r.scissorStart(0, 0, getWindowWidth(), getWindowHeight());
        r.quad(0, 0, getWindowWidth(), getWindowHeight(), new Color(0, 0, 0, (int) (130 * fade)));
        r.scissorEnd();

        r.glow(x, y + 8, w, h, 24, new Color(0, 0, 0, (int) (130 * fade)), false);
        r.glow(x, y, w, h, 34, accentAlpha((int) (160 * fade)), false);
        r.roundedRect(x, y, w, h, RADIUS, alpha(PANEL_BG, fade));

        drawHeader(r, x, y, w, fade, dt);
        drawRows(r, rows, x, w, bodyTop, bodyBottom, fade, dt);
        drawScrollbar(r, x, w, bodyTop, bodyBottom, contentH, fade);

        // Footer
        r.quad(x + PAD, bodyBottom, w - PAD * 2, 1, alpha(DIVIDER, fade));
        text("Right-click a setting to reset it   ·   Esc to close", x + w / 2, bodyBottom + (FOOTER_H - 9) / 2,
            alpha(TEXT_FAINT, fade), SMALL_SCALE, 2, 0);

        r.end();
        flushText(graphics);
    }

    private void drawHeader(GuiRenderer r, double x, double y, double w, float fade, float dt) {
        r.roundedRect(x, y, w, HEADER_H + RADIUS, RADIUS, alpha(HEADER_BG, fade));
        r.quad(x, y + HEADER_H - RADIUS, w, RADIUS, alpha(HEADER_BG, fade));
        r.quad(x, y + HEADER_H, w, 1, alpha(DIVIDER, fade));

        text(target.title(), x + PAD, y + 14, alpha(TEXT, fade), TITLE_SCALE, 0, 0);
        text(target.description(), x + PAD, y + 38, alpha(TEXT_DIM, fade), SMALL_SCALE, 0, w - PAD * 2 - 110);

        // Close button
        double cs = 26, cx = x + w - PAD - cs, cy = y + (HEADER_H - cs) / 2;
        boolean closeHover = over(cx, cy, cs, cs);
        float ca = anim("close", closeHover, dt);
        if (ca > 0.01f) r.roundedRect(cx, cy, cs, cs, cs / 2, new Color(DANGER.r, DANGER.g, DANGER.b, (int) (70 * ca * fade)));
        text("x", cx + cs / 2, cy + 7, alpha(closeHover ? TEXT : TEXT_DIM, fade), LABEL_SCALE, 2, 0);
        hits.add(new Hit(cx, cy, cs, cs, null, "close"));

        // Active toggle
        double tw = 40, th = 22, tx = cx - 14 - tw, ty = y + (HEADER_H - th) / 2;
        drawToggle(r, "active", target.active().getAsBoolean(), tx, ty, tw, th, fade, dt);
        hits.add(new Hit(tx, ty, tw, th, null, "active"));
    }

    private void drawRows(GuiRenderer r, List<Row> rows, double x, double w, double top, double bottom, float fade, float dt) {
        double rowY = top + 4 - scroll;
        double ctrlX = x + w - PAD - CTRL_W;

        for (Row row : rows) {
            double rh = row.height();
            boolean visible = rowY >= top - 0.5 && rowY + rh <= bottom + 0.5;

            if (visible) {
                if (row.kind() == Kind.GROUP) {
                    text(row.title(), x + PAD, rowY + 14, alpha(accent(), fade), SMALL_SCALE + 0.06, 0, 0);
                    r.quad(x + PAD, rowY + rh - 5, w - PAD * 2, 1, alpha(DIVIDER, fade));
                } else {
                    drawRow(r, row, x, w, rowY, ctrlX, fade, dt);
                }
            }

            rowY += rh;
        }
    }

    private void drawRow(GuiRenderer r, Row row, double x, double w, double rowY, double ctrlX, float fade, float dt) {
        double rh = row.height();
        boolean sub = row.kind() == Kind.CHANNEL || row.kind() == Kind.RAINBOW;

        boolean hover = over(x + 8, rowY, w - 16, rh);
        float ha = anim(row, hover, dt);
        if (ha > 0.01f) r.roundedRect(x + 8, rowY + 2, w - 16, rh - 4, 9, new Color(255, 255, 255, (int) (10 * ha * fade)));

        hits.add(new Hit(x + 8, rowY, ctrlX - x - 16, rh, row, "label"));

        double labelX = x + PAD + (sub ? 14 : 0);

        if (sub) {
            text(row.title(), labelX, rowY + (rh - 9) / 2, alpha(TEXT_DIM, fade), LABEL_SCALE, 0, 0);
        } else {
            boolean changed = isChanged(row);
            if (changed) r.quad(labelX - 10, rowY + 14, 5, 5, GuiRenderer.CIRCLE, alpha(accent(), fade));

            text(row.title(), labelX, rowY + 8, alpha(TEXT, fade), LABEL_SCALE, 0, ctrlX - labelX - 16);
            if (row.description() != null && !row.description().isEmpty()) {
                text(row.description(), labelX, rowY + 26, alpha(TEXT_FAINT, fade), SMALL_SCALE, 0, ctrlX - labelX - 16);
            }
        }

        double cy = rowY + rh / 2;

        switch (row.kind()) {
            case BOOL -> {
                double tw = 40, th = 22, tx = ctrlX + CTRL_W - tw;
                drawToggle(r, row, (Boolean) row.setting().get(), tx, cy - th / 2, tw, th, fade, dt);
                hits.add(new Hit(tx, cy - th / 2, tw, th, row, "toggle"));
            }
            case INT, DOUBLE, CHANNEL -> drawSlider(r, row, ctrlX, cy, fade, dt);
            case ENUM -> drawEnum(r, row, ctrlX, cy, fade, dt);
            case STRING -> drawString(r, row, ctrlX, cy, fade, dt);
            case KEYBIND, MODULE_BIND -> drawKeybind(r, row, ctrlX, cy, fade, dt);
            case COLOR -> drawColor(r, row, ctrlX, cy, fade, dt);
            case RAINBOW -> {
                double tw = 34, th = 18, tx = ctrlX + CTRL_W - tw;
                drawToggle(r, row, ((SettingColor) row.setting().get()).rainbow, tx, cy - th / 2, tw, th, fade, dt);
                hits.add(new Hit(tx, cy - th / 2, tw, th, row, "rainbow"));
            }
            case OTHER -> {
                double bh = 28, by = cy - bh / 2;
                boolean bHover = over(ctrlX, by, CTRL_W, bh);
                float ba = anim(row + "btn", bHover, dt);
                r.roundedRect(ctrlX, by, CTRL_W, bh, 8, alpha(lerp(CONTROL_BG, CONTROL_HOVER, ba), fade));
                text("Open editor", ctrlX + CTRL_W / 2, by + (bh - 9) / 2, alpha(accent(), fade), LABEL_SCALE, 2, 0);
                hits.add(new Hit(ctrlX, by, CTRL_W, bh, row, "editor"));
            }
            default -> {
            }
        }
    }

    private void drawToggle(GuiRenderer r, Object key, boolean on, double x, double y, double w, double h, float fade, float dt) {
        float a = anim(key + "tgl", on, dt * 1.4f);
        r.roundedRect(x, y, w, h, h / 2, alpha(lerp(TOGGLE_OFF, accent(), a), fade));
        double knob = h - 6;
        r.quad(x + 3 + (w - knob - 6) * a, y + 3, knob, knob, GuiRenderer.CIRCLE, alpha(TEXT, fade));
    }

    private void drawSlider(GuiRenderer r, Row row, double ctrlX, double cy, float fade, float dt) {
        double boxW = 54, boxH = 24;
        double trackX = ctrlX, trackW = CTRL_W - boxW - 12, trackH = 4;
        double pct = sliderPct(row);

        boolean hover = over(trackX - 6, cy - 12, trackW + 12, 24) || dragRow == row;
        float ha = anim(row + "sld", hover, dt);

        r.roundedRect(trackX, cy - trackH / 2, trackW, trackH, trackH / 2, alpha(TRACK, fade));
        if (trackW * pct > 1) r.roundedRect(trackX, cy - trackH / 2, trackW * pct, trackH, trackH / 2, alpha(accent(), fade));

        double knob = 12 + 3 * ha;
        double knobX = trackX + trackW * pct - knob / 2, knobY = cy - knob / 2;
        if (ha > 0.01f) r.glow(knobX, knobY, knob, knob, 7, accentAlpha((int) (120 * ha * fade)), true);
        r.quad(knobX, knobY, knob, knob, GuiRenderer.CIRCLE, alpha(TEXT, fade));

        hits.add(new Hit(trackX - 6, cy - 12, trackW + 12, 24, row, "slider"));

        double boxX = ctrlX + CTRL_W - boxW;
        r.roundedRect(boxX, cy - boxH / 2, boxW, boxH, 7, alpha(CONTROL_BG, fade));
        text(sliderText(row), boxX + boxW / 2, cy - 4.5, alpha(TEXT, fade), SMALL_SCALE + 0.06, 2, 0);
    }

    private void drawEnum(GuiRenderer r, Row row, double ctrlX, double cy, float fade, float dt) {
        double bh = 28, by = cy - bh / 2, arrow = 30;

        boolean leftHover = over(ctrlX, by, arrow, bh);
        boolean rightHover = over(ctrlX + CTRL_W - arrow, by, arrow, bh);
        float ha = anim(row + "enm", over(ctrlX, by, CTRL_W, bh), dt);

        r.roundedRect(ctrlX, by, CTRL_W, bh, 8, alpha(lerp(CONTROL_BG, CONTROL_HOVER, ha), fade));

        Color left = alpha(leftHover ? accent() : TEXT_DIM, fade);
        Color right = alpha(rightHover ? accent() : TEXT_DIM, fade);
        double ax = ctrlX + arrow / 2, bx = ctrlX + CTRL_W - arrow / 2;
        r.triangle(ax + 3, cy - 5, ax + 3, cy + 5, ax - 3, cy, left);
        r.triangle(bx - 3, cy - 5, bx - 3, cy + 5, bx + 3, cy, right);

        text(prettyEnum(row.setting().get()), ctrlX + CTRL_W / 2, cy - 4.5, alpha(TEXT, fade), LABEL_SCALE - 0.06, 2, CTRL_W - arrow * 2 - 8);

        hits.add(new Hit(ctrlX, by, arrow + (CTRL_W - arrow * 2) / 2, bh, row, "enumPrev"));
        hits.add(new Hit(ctrlX + arrow + (CTRL_W - arrow * 2) / 2, by, arrow + (CTRL_W - arrow * 2) / 2, bh, row, "enumNext"));
    }

    private void drawString(GuiRenderer r, Row row, double ctrlX, double cy, float fade, float dt) {
        double bh = 28, by = cy - bh / 2;
        boolean focused = editing == row.setting();
        float fa = anim(row + "str", focused, dt);

        if (fa > 0.01f) r.glow(ctrlX, by, CTRL_W, bh, 8, accentAlpha((int) (130 * fa * fade)), false);
        r.roundedRect(ctrlX, by, CTRL_W, bh, 8, alpha(CONTROL_BG, fade));

        String value = focused ? editBuffer : String.valueOf(row.setting().get());
        boolean caret = focused && (System.currentTimeMillis() / 500) % 2 == 0;

        if (value.isEmpty() && !focused) {
            text("Empty", ctrlX + 10, cy - 4.5, alpha(TEXT_FAINT, fade), LABEL_SCALE - 0.06, 0, CTRL_W - 20);
        } else {
            // Show the end of long text so the caret stays visible while typing.
            text(value + (caret ? "|" : ""), ctrlX + CTRL_W - 10, cy - 4.5, alpha(TEXT, fade), LABEL_SCALE - 0.06, 3, CTRL_W - 20);
        }

        hits.add(new Hit(ctrlX, by, CTRL_W, bh, row, "string"));
    }

    private void drawKeybind(GuiRenderer r, Row row, double ctrlX, double cy, float fade, float dt) {
        double bh = 28, by = cy - bh / 2;
        Keybind keybind = keybindOf(row);
        boolean listening = capturing != null && capturing == keybind;
        float la = anim(row + "kb", listening, dt);
        float ha = anim(row + "kbh", over(ctrlX, by, CTRL_W, bh), dt);

        if (la > 0.01f) r.glow(ctrlX, by, CTRL_W, bh, 8, accentAlpha((int) (130 * la * fade)), false);
        r.roundedRect(ctrlX, by, CTRL_W, bh, 8, alpha(lerp(CONTROL_BG, CONTROL_HOVER, ha), fade));

        String label = listening ? "Press a key..." : (keybind.isSet() ? keybind.toString() : "None");
        Color color = listening ? accent() : (keybind.isSet() ? TEXT : TEXT_FAINT);
        text(label, ctrlX + CTRL_W / 2, cy - 4.5, alpha(color, fade), LABEL_SCALE - 0.06, 2, CTRL_W - 16);

        hits.add(new Hit(ctrlX, by, CTRL_W, bh, row, "keybind"));
    }

    private void drawColor(GuiRenderer r, Row row, double ctrlX, double cy, float fade, float dt) {
        SettingColor color = (SettingColor) row.setting().get();
        double bh = 28, by = cy - bh / 2, swatch = 20;
        boolean expanded = expandedColors.contains(row.setting());
        float ha = anim(row + "clr", over(ctrlX, by, CTRL_W, bh), dt);
        float ea = anim(row + "clrexp", expanded, dt);

        r.roundedRect(ctrlX, by, CTRL_W, bh, 8, alpha(lerp(CONTROL_BG, CONTROL_HOVER, ha), fade));

        double sx = ctrlX + CTRL_W - swatch - 6;
        r.roundedRect(sx - 1, cy - swatch / 2 - 1, swatch + 2, swatch + 2, 6, alpha(DIVIDER, fade));
        r.roundedRect(sx, cy - swatch / 2, swatch, swatch, 5, new Color(color.r, color.g, color.b, (int) (255 * fade)));

        text(String.format("#%02X%02X%02X", color.r, color.g, color.b), ctrlX + 12, cy - 4.5, alpha(TEXT, fade), LABEL_SCALE - 0.06, 0, 0);

        double ax = sx - 14;
        r.triangle(ax - 4, cy - 2 + ea * 4, ax + 4, cy - 2 + ea * 4, ax, cy + 3 - ea * 6, alpha(TEXT_DIM, fade));

        hits.add(new Hit(ctrlX, by, CTRL_W, bh, row, "color"));
    }

    private void drawScrollbar(GuiRenderer r, double x, double w, double top, double bottom, double contentH, float fade) {
        if (maxScroll <= 0) return;

        double trackH = bottom - top - 12;
        double thumbH = Math.max(28, trackH * (trackH / (contentH + 8)));
        double thumbY = top + 6 + (trackH - thumbH) * (scroll / maxScroll);
        r.roundedRect(x + w - 7, thumbY, 3, thumbH, 1.5, accentAlpha((int) (110 * fade)));
    }

    // --- Text ---

    /** align: 0 left, 1 right-anchored, 2 centred on x, 3 right-anchored keeping the end visible. */
    private void text(String s, double x, double y, Color color, double scale, int align, double maxWidth) {
        if (s == null || s.isEmpty()) return;
        texts.add(new TextOp(s, x, y, color, scale, align, maxWidth));
    }

    private void flushText(GuiGraphicsExtractor graphics) {
        Map<Double, List<TextOp>> byScale = new TreeMap<>();
        for (TextOp op : texts) byScale.computeIfAbsent(op.scale(), k -> new ArrayList<>()).add(op);

        for (Map.Entry<Double, List<TextOp>> entry : byScale.entrySet()) {
            theme.textRenderer().begin(graphics, theme.scale(entry.getKey()));
            double lineH = theme.textHeight();

            for (TextOp op : entry.getValue()) {
                String s = op.box() > 0 ? fit(op.text(), op.box(), op.align() == 3) : op.text();
                double tw = theme.textWidth(s);

                double drawX = switch (op.align()) {
                    case 1, 3 -> op.x() - tw;
                    case 2 -> op.x() - tw / 2;
                    default -> op.x();
                };

                // Callers pass y as the top of a 9px-tall line; centre the real line height there.
                double drawY = op.y() + (9 - lineH) / 2;
                theme.textRenderer().render(s, drawX, drawY, op.color(), false);
            }

            theme.textRenderer().end();
        }
    }

    private String fit(String s, double maxWidth, boolean keepEnd) {
        if (theme.textWidth(s) <= maxWidth) return s;

        StringBuilder sb = new StringBuilder(s);
        while (sb.length() > 1 && theme.textWidth((keepEnd ? "..." + sb : sb + "...")) > maxWidth) {
            if (keepEnd) sb.deleteCharAt(0);
            else sb.deleteCharAt(sb.length() - 1);
        }
        return keepEnd ? "..." + sb : sb + "...";
    }

    // --- Values ---

    private double sliderPct(Row row) {
        if (row.kind() == Kind.CHANNEL) return channelValue((SettingColor) row.setting().get(), row.channel()) / 255.0;

        if (row.setting() instanceof IntSetting is) {
            int lo = is.sliderMin, hi = is.sliderMax;
            return hi <= lo ? 0.5 : Mth.clamp((double) (is.get() - lo) / (hi - lo), 0, 1);
        }
        if (row.setting() instanceof DoubleSetting ds) {
            double lo = ds.sliderMin, hi = ds.sliderMax;
            if (hi <= lo || !Double.isFinite(lo) || !Double.isFinite(hi)) return 0.5;
            return Mth.clamp((ds.get() - lo) / (hi - lo), 0, 1);
        }
        return 0;
    }

    private String sliderText(Row row) {
        if (row.kind() == Kind.CHANNEL) return String.valueOf(channelValue((SettingColor) row.setting().get(), row.channel()));
        if (row.setting() instanceof IntSetting is) return String.valueOf(is.get());
        if (row.setting() instanceof DoubleSetting ds) {
            double v = ds.get();
            if (!Double.isFinite(v)) return "-";
            return String.format("%." + Mth.clamp(ds.decimalPlaces, 0, 2) + "f", v);
        }
        return "";
    }

    private void applySlider(Row row, Hit hit, double px) {
        if (hit == null) return;

        double trackW = CTRL_W - 54 - 12;
        double trackX = hit.x() + 6;
        double pct = Mth.clamp((px - trackX) / trackW, 0, 1);

        if (row.kind() == Kind.CHANNEL) {
            @SuppressWarnings("unchecked")
            Setting<SettingColor> cs = (Setting<SettingColor>) row.setting();
            SettingColor c = cs.get();
            int v = (int) Math.round(pct * 255);
            int[] rgba = {c.r, c.g, c.b, c.a};
            rgba[row.channel()] = v;
            c.set(new Color(rgba[0], rgba[1], rgba[2], rgba[3]));
            cs.onChanged();
        } else if (row.setting() instanceof IntSetting is) {
            int v = (int) Math.round(is.sliderMin + pct * (is.sliderMax - is.sliderMin));
            is.set(Mth.clamp(v, is.min, is.max));
        } else if (row.setting() instanceof DoubleSetting ds) {
            if (!Double.isFinite(ds.sliderMin) || !Double.isFinite(ds.sliderMax)) return;
            double factor = Math.pow(10, Mth.clamp(ds.decimalPlaces, 0, 3));
            double v = Math.round((ds.sliderMin + pct * (ds.sliderMax - ds.sliderMin)) * factor) / factor;
            ds.set(Mth.clamp(v, ds.min, ds.max));
        }
    }

    private static int channelValue(SettingColor c, int channel) {
        return switch (channel) {
            case 0 -> c.r;
            case 1 -> c.g;
            case 2 -> c.b;
            default -> c.a;
        };
    }

    private boolean isChanged(Row row) {
        if (row.setting() == null) return false;
        return !Objects.equals(row.setting().get(), row.setting().getDefaultValue());
    }

    private Keybind keybindOf(Row row) {
        if (row.kind() == Kind.MODULE_BIND) return target.keybind();
        return (Keybind) row.setting().get();
    }

    private static String prettyEnum(Object value) {
        String s = String.valueOf(value);
        return s.isEmpty() ? s : s.replace('_', ' ');
    }

    private static Color lerp(Color a, Color b, float t) {
        return new Color(
            (int) Mth.lerp(t, a.r, b.r),
            (int) Mth.lerp(t, a.g, b.g),
            (int) Mth.lerp(t, a.b, b.b),
            (int) Mth.lerp(t, a.a, b.a));
    }

    private boolean over(double x, double y, double w, double h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // --- Input ---

    public boolean mouseClicked(double cx, double cy, int button) {
        if (target == null || closing) return true;

        mouseDown = true;

        Hit hit = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).contains(cx, cy)) {
                hit = hits.get(i);
                break;
            }
        }

        if (editing != null && (hit == null || hit.row() == null || hit.row().setting() != editing)) stopEditing(true);
        if (capturing != null && (hit == null || !"keybind".equals(hit.area()))) capturing = null;

        if (hit == null) {
            // Clicking outside the panel closes it, like a normal popup.
            double w = Math.min(MAX_W, getWindowWidth() - 40);
            double x = (getWindowWidth() - w) / 2;
            if (cx < x || cx > x + w) close();
            return true;
        }

        Row row = hit.row();

        switch (hit.area()) {
            case "close" -> close();
            case "active" -> target.toggle().run();
            case "label" -> {
                if (button == 1 && row != null) reset(row);
            }
            case "toggle" -> {
                BoolSetting bs = (BoolSetting) row.setting();
                if (button == 1) bs.reset();
                else bs.set(!bs.get());
            }
            case "rainbow" -> {
                @SuppressWarnings("unchecked")
                Setting<SettingColor> cs = (Setting<SettingColor>) row.setting();
                cs.get().rainbow = !cs.get().rainbow;
                cs.onChanged();
            }
            case "slider" -> {
                if (button == 1) {
                    reset(row);
                } else {
                    dragRow = row;
                    dragHit = hit;
                    applySlider(row, hit, cx);
                }
            }
            case "enumPrev", "enumNext" -> {
                if (button == 1) reset(row);
                else cycleEnum(row, hit.area().equals("enumNext") ? 1 : -1);
            }
            case "string" -> {
                if (button == 1) {
                    reset(row);
                } else if (editing != row.setting()) {
                    editing = (StringSetting) row.setting();
                    editBuffer = editing.get();
                }
            }
            case "keybind" -> {
                Keybind keybind = keybindOf(row);
                if (button == 1) {
                    keybind.reset();
                    if (row.setting() != null) row.setting().onChanged();
                    capturing = null;
                } else {
                    capturing = keybind;
                    capturingSetting = row.setting();
                }
            }
            case "color" -> {
                if (button == 1) {
                    reset(row);
                } else if (!expandedColors.remove(row.setting())) {
                    expandedColors.add(row.setting());
                }
            }
            case "editor" -> {
                Runnable openEditor = target.openNativeEditor();
                close();
                if (openEditor != null) openEditor.run();
            }
            default -> {
            }
        }

        return true;
    }

    public void mouseReleased() {
        mouseDown = false;
        dragRow = null;
        dragHit = null;
    }

    public boolean mouseScrolled(double amount) {
        if (target == null) return false;
        scroll = Mth.clamp(scroll - amount * ROW_H * 0.8, 0, maxScroll);
        return true;
    }

    public boolean keyPressed(KeyEvent input) {
        if (target == null) return false;
        int key = input.key();

        if (capturing != null) {
            if (key == GLFW_KEY_ESCAPE) {
                capturing = null;
            } else if (key == GLFW_KEY_BACKSPACE || key == GLFW_KEY_DELETE) {
                capturing.reset();
                finishCapture();
            } else {
                capturing.set(true, key, input.modifiers());
                finishCapture();
            }
            return true;
        }

        if (editing != null) {
            switch (key) {
                case GLFW_KEY_ESCAPE -> stopEditing(false);
                case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> stopEditing(true);
                case GLFW_KEY_BACKSPACE -> {
                    if (!editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
                }
                default -> {
                }
            }
            return true;
        }

        if (key == GLFW_KEY_ESCAPE) close();
        return true;
    }

    public boolean charTyped(CharacterEvent input) {
        if (target == null) return false;
        if (editing == null) return true;

        char c = (char) input.codepoint();
        if (c >= 32 && editBuffer.length() < 256) editBuffer += c;
        return true;
    }

    private void finishCapture() {
        if (capturingSetting != null) capturingSetting.onChanged();
        capturing = null;
        capturingSetting = null;
    }

    private void stopEditing(boolean commit) {
        if (editing != null && commit) editing.set(editBuffer);
        editing = null;
        editBuffer = "";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void cycleEnum(Row row, int direction) {
        EnumSetting setting = (EnumSetting) row.setting();
        Enum<?> current = (Enum<?>) setting.get();
        Object[] values = current.getDeclaringClass().getEnumConstants();

        int index = (current.ordinal() + direction + values.length) % values.length;
        setting.set(values[index]);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void reset(Row row) {
        if (row.setting() != null) {
            if (row.kind() == Kind.CHANNEL || row.kind() == Kind.RAINBOW) return;
            ((Setting) row.setting()).reset();
        }
    }
}
