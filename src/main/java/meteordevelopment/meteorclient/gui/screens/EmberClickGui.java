package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.XAnchor;
import meteordevelopment.meteorclient.systems.hud.YAnchor;
import meteordevelopment.meteorclient.systems.hud.screens.HudElementScreen;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.EmberAnim;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.EmberPalette;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static org.lwjgl.glfw.GLFW.*;

public class EmberClickGui extends TabScreen {

    private static final Color BG_OVERLAY = new Color(0, 0, 0, 90);
    private static final Color DOT_ON = new Color(93, 217, 127, 255);

    // Refreshed from EmberPalette every frame, so switching themes recolours everything.
    private static Color ACCENT, PANEL_BG, HEADER_BG, DIVIDER, ROW_ACTIVE, HEADER_TEXT,
        TEXT_WHITE, TEXT_INACTIVE, TEXT_FAINT, SEARCH_BG, DOT_OFF;

    static {
        syncPalette();
    }

    private static final double PW = 240;
    private static final double HH = 34;
    private static final double MH = 31;
    private static final double PR = 9;
    private static final double GAP = 18;

    /** Width of the "settings" handle at the right of a row that opens the popup. */
    private static final double HANDLE_W = 40;

    private final List<Panel> panels = new ArrayList<>();
    private Panel dragging = null;
    private double dragOX, dragOY;
    private double mx, my;
    private String search = "";
    private boolean searchFocused = false;
    private float globalFade = 0f;
    private final EmberAnim.Clock clock = new EmberAnim.Clock();
    private float frameDt;

    private final Map<Object, Float> hoverAnims = new HashMap<>();
    private final Map<Object, Float> activeAnims = new HashMap<>();

    private final EmberSettingsPopup popup;

    private static final Map<String, double[]> saved = new HashMap<>();

    public EmberClickGui(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
        popup = new EmberSettingsPopup(theme);
        buildPanels();
    }

    private static void syncPalette() {
        ACCENT = EmberPalette.accent();
        PANEL_BG = EmberPalette.panel();
        HEADER_BG = EmberPalette.header();
        DIVIDER = EmberPalette.divider();
        ROW_ACTIVE = EmberPalette.rowActive();
        HEADER_TEXT = EmberPalette.textBright();
        TEXT_WHITE = EmberPalette.textBright();
        TEXT_INACTIVE = EmberPalette.textDim();
        TEXT_FAINT = EmberPalette.textFaint();
        SEARCH_BG = EmberPalette.search();
        DOT_OFF = EmberPalette.dotOff();
    }

    private void buildPanels() {
        panels.clear();
        double sx = 10, sy = 36;

        for (Category cat : Modules.loopCategories()) {
            List<Module> mods = Modules.get().getGroup(cat);
            if (mods.isEmpty()) continue;

            Panel p = new Panel();
            p.category = cat;
            p.name = cat.name;
            p.modules = mods;
            p.icon = cat.icon.get();

            double[] s = saved.get(p.name);
            if (s != null) {
                p.x = s[0]; p.y = s[1]; p.collapsed = s[2] > 0;
            } else {
                p.x = sx; p.y = sy;
                sx += PW + GAP;
                if (sx + PW > getWindowWidth() - 10) { sx = 10; sy += 340; }
            }
            panels.add(p);
        }

        // Client panel - only top bar + spotify
        {
            Panel cp = new Panel();
            cp.name = "Client";
            cp.isClient = true;
            cp.icon = new ItemStack(Items.ENDER_EYE);
            List<ClientEntry> entries = new ArrayList<>();
            String[] wanted = {"ember-top-bar", "spotify", "ember-module-list", "ember-notifications"};
            for (String wName : wanted) {
                HudElement found = null;
                for (HudElement el : Hud.get()) {
                    if (el.info.name.equals(wName)) { found = el; break; }
                }
                if (found == null) {
                    var info = Hud.get().infos.get(wName);
                    if (info != null) {
                        switch (wName) {
                            case "ember-module-list" -> {
                                // Meteor's plain list sits in the same corner; the Ember one replaces it.
                                for (HudElement el : Hud.get()) {
                                    if (el.info.name.equals("active-modules") && el.isActive()) el.toggle();
                                }
                                Hud.get().add(info, -4, 4, XAnchor.Right, YAnchor.Top);
                            }
                            case "ember-notifications" -> Hud.get().add(info, -4, -40, XAnchor.Right, YAnchor.Bottom);
                            default -> Hud.get().add(info, 4, wName.equals("spotify") ? 30 : 4);
                        }
                        for (HudElement el : Hud.get()) {
                            if (el.info.name.equals(wName)) { found = el; break; }
                        }
                    }
                }
                if (found != null) {
                    entries.add(new ClientEntry(found.info.title, found));
                }
            }

            entries.add(new ClientEntry("Edit HUD Positions",
                () -> mc.gui.setScreen(new meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen(theme))));

            cp.clientEntries = entries;

            double[] s = saved.get("Client");
            if (s != null) {
                cp.x = s[0]; cp.y = s[1]; cp.collapsed = s[2] > 0;
            } else {
                cp.x = sx; cp.y = sy;
                sx += PW + GAP;
                if (sx + PW > getWindowWidth() - 10) { sx = 10; sy += 340; }
            }
            panels.add(cp);
        }

        // Theme panel
        {
            Panel tp = new Panel();
            tp.name = "Theme";
            tp.isTheme = true;
            tp.icon = new ItemStack(Items.PAINTING);

            double[] s = saved.get("Theme");
            if (s != null) {
                tp.x = s[0]; tp.y = s[1]; tp.collapsed = s[2] > 0;
            } else {
                tp.x = sx; tp.y = sy;
            }
            panels.add(tp);
        }
    }

    @Override
    public void initWidgets() {
        clear();
    }

    /** Real-time easing; a higher speed settles faster (speed 12 is about a fifth of a second). */
    private float anim(Object key, Map<Object, Float> map, float target, float speed, float dt) {
        float cur = map.getOrDefault(key, target == 1f ? 0f : target);
        cur = EmberAnim.approach(cur, target, dt, 0.8 / speed);
        map.put(key, cur);
        return cur;
    }

    @Override
    protected void onRenderBefore(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        syncPalette();
        frameDt = clock.tick();

        // Panels ignore the mouse while the popup is up, so nothing behind it lights up.
        boolean popupUp = popup.isVisible();
        mx = popupUp ? -10000 : mouseX;
        my = popupUp ? -10000 : mouseY;

        globalFade = Math.min(1f, globalFade + frameDt / 0.25f);
        float fade = easeOut(globalFade);

        if (dragging != null) {
            dragging.x = mouseX - dragOX;
            dragging.y = mouseY - dragOY;
        }

        for (Panel p : panels) {
            float openTarget = p.collapsed ? 0f : 1f;
            p.openAnim = EmberAnim.approach(p.openAnim, openTarget, frameDt, 0.07);
            p.bodyH = computeBodyH(p);
        }

        GuiRenderer r = new GuiRenderer();
        r.theme = theme;
        r.begin(graphics);

        // Dim background
        // Own batch so it is flushed before the glow pass instead of darkening it.
        r.scissorStart(0, 0, getWindowWidth(), getWindowHeight());
        r.quad(0, 0, getWindowWidth(), getWindowHeight(), new Color(0, 0, 0, (int)(BG_OVERLAY.a * fade)));
        r.scissorEnd();

        // Every panel's halo is flushed before any panel body, so one panel's glow can never
        // tint the edge of the panel next to it.
        r.scissorStart(0, 0, getWindowWidth(), getWindowHeight());
        for (Panel p : panels) drawPanelHalo(r, p, fade);
        r.scissorEnd();

        for (Panel p : panels) {
            if (p.isTheme) drawThemePanel(r, graphics, p, delta, fade);
            else if (p.isClient) drawClientPanel(r, graphics, p, delta, fade);
            else drawPanel(r, graphics, p, delta, fade);
        }

        drawSearchBar(r, fade);
        drawConfigButton(r, delta, fade);
        drawGearButton(r, fade);
        r.end();

        for (Panel p : panels) {
            if (p.isTheme) drawThemePanelText(graphics, p);
            else if (p.isClient) drawClientPanelText(graphics, p);
            else drawPanelText(graphics, p);
        }
        drawSearchText(graphics);
        drawConfigButtonText(graphics);

        popup.render(graphics, mouseX, mouseY, delta);
    }

    private double computeBodyH(Panel p) {
        if (p.collapsed && p.openAnim <= 0.01f) return 0;
        if (p.isTheme) return EmberPalette.NAMES.length * MH;
        if (p.isClient) return p.clientEntries == null ? 0 : Math.min(p.clientEntries.size() * MH, bodyLimit(p));
        return Math.min(filtered(p).size() * MH, bodyLimit(p));
    }

    private static final double CFG_W = 128;
    private static final double CFG_H = 32;

    private double configButtonX() {
        return (getWindowWidth() - CFG_W) / 2;
    }

    private double configButtonY() {
        return getWindowHeight() - CFG_H - 26;
    }

    private void drawConfigButton(GuiRenderer r, float delta, float fade) {
        double bx = configButtonX(), by = configButtonY();
        boolean hover = mx >= bx && mx < bx + CFG_W && my >= by && my < by + CFG_H;
        float hA = anim("cfgbtn", hoverAnims, hover ? 1f : 0f, 12f, frameDt);

        r.glow(bx, by + 3, CFG_W, CFG_H, 8, new Color(0, 0, 0, (int)(90 * fade)), false);
        if (hA > 0.01f) r.glow(bx, by, CFG_W, CFG_H, 16, accentAlpha((int)(150 * hA * fade)), false);

        Color bg = new Color(
            (int) Mth.lerp(hA, HEADER_BG.r, Math.min(255, HEADER_BG.r + 16)),
            (int) Mth.lerp(hA, HEADER_BG.g, Math.min(255, HEADER_BG.g + 14)),
            (int) Mth.lerp(hA, HEADER_BG.b, Math.min(255, HEADER_BG.b + 20)),
            (int)(248 * fade));
        r.roundedRect(bx, by, CFG_W, CFG_H, CFG_H / 2, bg);

        // Small folder glyph
        double gx = bx + 18, gy = by + CFG_H / 2;
        Color gc = accentAlpha((int)(210 + 45 * hA));
        r.roundedRect(gx - 7, gy - 5, 14, 10, 2, gc);
        r.roundedRect(gx - 7, gy - 7, 6, 3, 1, gc);
    }

    private static final double GEAR = 26;

    private double gearX() {
        return configButtonX() + CFG_W + 8;
    }

    private double gearY() {
        return configButtonY() + (CFG_H - GEAR) / 2;
    }

    /** Small round settings button beside Configs, opening Ember's own settings screen. */
    private void drawGearButton(GuiRenderer r, float fade) {
        double bx = gearX(), by = gearY();
        boolean hover = mx >= bx && mx < bx + GEAR && my >= by && my < by + GEAR;
        float hA = anim("gearbtn", hoverAnims, hover ? 1f : 0f, 12f, frameDt);

        r.glow(bx, by + 3, GEAR, GEAR, 8, new Color(0, 0, 0, (int)(90 * fade)), false);
        if (hA > 0.01f) r.glow(bx, by, GEAR, GEAR, 14, accentAlpha((int)(150 * hA * fade)), false);

        Color bg = new Color(
            (int) Mth.lerp(hA, HEADER_BG.r, Math.min(255, HEADER_BG.r + 16)),
            (int) Mth.lerp(hA, HEADER_BG.g, Math.min(255, HEADER_BG.g + 14)),
            (int) Mth.lerp(hA, HEADER_BG.b, Math.min(255, HEADER_BG.b + 20)),
            (int)(248 * fade));
        r.quad(bx, by, GEAR, GEAR, GuiRenderer.CIRCLE, bg);

        // Gear glyph: a ring with four teeth that turn a little as it lights up.
        double cx = bx + GEAR / 2, cy = by + GEAR / 2;
        Color gc = accentAlpha((int) Math.min(255, (200 + 55 * hA) * fade));

        double tooth = 3.2, reach = 6.6;
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(45 * hA + i * 90);
            r.roundedRect(cx + Math.cos(a) * reach - tooth / 2, cy + Math.sin(a) * reach - tooth / 2,
                tooth, tooth, 1, gc);
        }

        r.quad(cx - 4.5, cy - 4.5, 9, 9, GuiRenderer.CIRCLE, gc);
        r.quad(cx - 2, cy - 2, 4, 4, GuiRenderer.CIRCLE, bg);
    }

    private void drawConfigButtonText(GuiGraphicsExtractor gfx) {
        double bx = configButtonX(), by = configButtonY();

        theme.textRenderer().begin(gfx, theme.scale(0.95));
        String label = "Configs";
        double tw = theme.textWidth(label);
        theme.textRenderer().render(label, bx + 30 + ((CFG_W - 30) - tw) / 2 - 6,
            by + (CFG_H - theme.textHeight()) / 2, TEXT_WHITE, false);
        theme.textRenderer().end();
    }

    private float easeOut(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    /** Shared so other Ember screens follow the selected theme. */
    public static Color accent() {
        return EmberPalette.accent();
    }

    /** Tabs grow to fill the screen height, stopping above the Configs button. */
    private double bodyLimit(Panel p) {
        return Math.max(MH * 5, getWindowHeight() - p.y - HH - CFG_H - 60);
    }

    private Color accentAlpha(int a) {
        return new Color(ACCENT.r, ACCENT.g, ACCENT.b, Math.min(255, Math.max(0, a)));
    }

    /** Soft accent halo plus a soft drop shadow, both drawn under every panel body. */
    private void drawPanelHalo(GuiRenderer r, Panel p, float fade) {
        double totalH = HH + p.bodyH * p.openAnim;
        r.glow(p.x, p.y + 5, PW, totalH, 14, new Color(0, 0, 0, (int)(110 * fade)), false);
        r.glow(p.x, p.y, PW, totalH, 26, accentAlpha((int)(150 * fade * 0.75f)), false);
    }

    private void drawPanelFrame(GuiRenderer r, GuiGraphicsExtractor gfx, Panel p, double bodyH, float fade) {
        double x = p.x, y = p.y;
        double totalH = HH + bodyH * p.openAnim;

        // Body
        if (p.openAnim > 0.01f && bodyH > 0) {
            r.roundedRect(x, y, PW, totalH, PR, withAlpha(PANEL_BG, fade));
        }

        // Header stays dark; only a hover lift, never an accent fill
        boolean hHover = mx >= x && mx < x + PW && my >= y && my < y + HH;
        float hAmt = anim("hdr_" + p.name, hoverAnims, hHover ? 1f : 0f, 10f, frameDt);
        Color hc = new Color(
            (int) Mth.lerp(hAmt, HEADER_BG.r, Math.min(255, HEADER_BG.r + 14)),
            (int) Mth.lerp(hAmt, HEADER_BG.g, Math.min(255, HEADER_BG.g + 12)),
            (int) Mth.lerp(hAmt, HEADER_BG.b, Math.min(255, HEADER_BG.b + 18)),
            (int)(HEADER_BG.a * fade));

        if (p.openAnim < 0.05f) {
            r.roundedRect(x, y, PW, HH, PR, hc);
        } else {
            r.roundedRect(x, y, PW, HH + PR, PR, hc);
            r.quad(x, y + HH, PW, PR, hc);
            r.quad(x, y + HH, PW, 1, withAlpha(DIVIDER, fade));
        }

        // Chevron
        double arX = x + PW - 15, arY = y + HH / 2;
        float rot = p.openAnim;
        r.triangle(arX - 3.5, arY - 2 + rot * 4, arX + 3.5, arY - 2 + rot * 4,
            arX, arY + 3 - rot * 6, new Color(TEXT_INACTIVE.r, TEXT_INACTIVE.g, TEXT_INACTIVE.b, (int)(220 * fade)));

        // Icon
        int ix = (int)(x + 9);
        int iy = (int)(y + (HH - 16) / 2);
        final ItemStack icon = p.icon;
        if (icon != null) {
            r.post(() -> RenderUtils.drawItem(gfx, icon, ix, iy, 0.85f, false, null, false));
        }
    }

    private static Color withAlpha(Color c, float fade) {
        return new Color(c.r, c.g, c.b, (int)(c.a * fade));
    }

    /** Accent glow around an enabled row, so turning something on lights it up. */
    private void activeGlow(GuiRenderer r, double x, double y, double w, double h, float amount) {
        r.glow(x, y, w, h, 14, accentAlpha((int)(125 * amount)), false);
    }

    /**
     * Hover highlight for a row. A halo is wrong here: on a row with no pill behind it
     * the glow's square edges show against the panel, so the row is tinted instead and
     * an accent edge grows in on the left.
     */
    private void rowHover(GuiRenderer r, double x, double y, double w, double h, float amount, boolean active) {
        double radius = h / 2;

        r.roundedRect(x, y, w, h, radius, accentAlpha((int)(30 * amount)));
        r.roundedRect(x, y, w, h, radius, new Color(255, 255, 255, (int)(12 * amount)));

        if (!active) {
            double barH = (h - 11) * amount;
            if (barH > 1) r.roundedRect(x + 3.5, y + (h - barH) / 2, 2.5, barH, 1.25, accentAlpha((int)(210 * amount)));
        }
    }

    /** Row pill for an enabled module, tinted by the theme. */
    private Color activeRowColor(float amount) {
        int tr = (int) Mth.lerp(0.12f, ROW_ACTIVE.r, ACCENT.r);
        int tg = (int) Mth.lerp(0.12f, ROW_ACTIVE.g, ACCENT.g);
        int tb = (int) Mth.lerp(0.12f, ROW_ACTIVE.b, ACCENT.b);
        return new Color(
            (int) Mth.lerp(amount, PANEL_BG.r, tr),
            (int) Mth.lerp(amount, PANEL_BG.g, tg),
            (int) Mth.lerp(amount, PANEL_BG.b, tb),
            (int)(235 * amount));
    }

    /** Vertical three-dot handle marking a row whose settings open in the popup. */
    private void drawSettingsHandle(GuiRenderer r, double cx, double cy, float hover, boolean hot) {
        Color c = hot ? accentAlpha(230) : new Color(TEXT_INACTIVE.r, TEXT_INACTIVE.g, TEXT_INACTIVE.b, (int)(110 + 110 * hover));
        for (int i = -1; i <= 1; i++) {
            r.quad(cx - 1.5, cy + i * 5 - 1.5, 3, 3, GuiRenderer.CIRCLE, c);
        }
    }

    private boolean overHandle(double x, double rowY) {
        return mx >= x + PW - HANDLE_W && mx < x + PW && my >= rowY && my < rowY + MH;
    }

    private void drawPanel(GuiRenderer r, GuiGraphicsExtractor gfx, Panel p, float delta, float fade) {
        List<Module> mods = filtered(p);
        drawPanelFrame(r, gfx, p, p.bodyH, fade);
        if (p.openAnim < 0.02f) return;

        double x = p.x, y = p.y;
        double visBody = p.bodyH * p.openAnim;
        double rowY = y + HH - p.scroll;
        double clipT = y + HH, clipB = y + HH + visBody;

        // Rows get their own batch so hover glows draw over the panel body but under the rows.
        r.scissorStart(0, 0, getWindowWidth(), getWindowHeight());

        for (Module m : mods) {
            if (rowY >= clipT - 0.5 && rowY + MH <= clipB + 0.5) {
                boolean hover = mx >= x && mx < x + PW && my >= rowY && my < rowY + MH && my < clipB;
                boolean active = m.isActive();
                float hA = anim(m, hoverAnims, hover ? 1f : 0f, 12f, frameDt);
                float aA = anim(m, activeAnims, active ? 1f : 0f, 8f, frameDt);

                if (aA > 0.01f) activeGlow(r, x + 4, rowY + 1, PW - 8, MH - 2, aA);
                if (aA > 0.01f) r.roundedRect(x + 4, rowY + 1, PW - 8, MH - 2, (MH - 2) / 2, activeRowColor(aA));
                if (hA > 0.01f) rowHover(r, x + 4, rowY + 1, PW - 8, MH - 2, hA, active);

                int dr = (int) Mth.lerp(aA, DOT_OFF.r, DOT_ON.r);
                int dg = (int) Mth.lerp(aA, DOT_OFF.g, DOT_ON.g);
                int db = (int) Mth.lerp(aA, DOT_OFF.b, DOT_ON.b);
                r.quad(x + PW - 17, rowY + (MH - 5) / 2, 5, 5, GuiRenderer.CIRCLE, new Color(dr, dg, db, 255));

                if (hasSettings(m.settings)) drawSettingsHandle(r, x + PW - 31, rowY + MH / 2, hA, overHandle(x, rowY));
            }
            rowY += MH;
        }

        r.scissorEnd();
    }

    private void drawClientPanel(GuiRenderer r, GuiGraphicsExtractor gfx, Panel p, float delta, float fade) {
        drawPanelFrame(r, gfx, p, p.bodyH, fade);
        if (p.openAnim < 0.02f || p.clientEntries == null) return;

        double x = p.x, y = p.y;
        double visBody = p.bodyH * p.openAnim;
        double rowY = y + HH - p.scroll;
        double clipT = y + HH, clipB = y + HH + visBody;

        r.scissorStart(0, 0, getWindowWidth(), getWindowHeight());

        for (ClientEntry entry : p.clientEntries) {
            if (rowY >= clipT - 0.5 && rowY + MH <= clipB + 0.5) {
                boolean hover = mx >= x && mx < x + PW && my >= rowY && my < rowY + MH && my < clipB;
                boolean active = entry.element != null && entry.element.isActive();
                float hA = anim("cl_" + entry.name, hoverAnims, hover ? 1f : 0f, 12f, frameDt);
                float aA = anim("cl_" + entry.name, activeAnims, active ? 1f : 0f, 8f, frameDt);

                if (aA > 0.01f) activeGlow(r, x + 4, rowY + 1, PW - 8, MH - 2, aA);
                if (aA > 0.01f) r.roundedRect(x + 4, rowY + 1, PW - 8, MH - 2, (MH - 2) / 2, activeRowColor(aA));
                if (hA > 0.01f) rowHover(r, x + 4, rowY + 1, PW - 8, MH - 2, hA, active);

                if (entry.element == null) {
                    // Action row (HUD editor) - arrow instead of a toggle dot
                    double ax = x + PW - 18, ay = rowY + MH / 2;
                    r.triangle(ax - 2, ay - 3, ax - 2, ay + 3, ax + 3, ay,
                        new Color(TEXT_INACTIVE.r, TEXT_INACTIVE.g, TEXT_INACTIVE.b, (int)(160 + 60 * hA)));
                } else {
                    int dr = (int) Mth.lerp(aA, DOT_OFF.r, DOT_ON.r);
                    int dg = (int) Mth.lerp(aA, DOT_OFF.g, DOT_ON.g);
                    int db = (int) Mth.lerp(aA, DOT_OFF.b, DOT_ON.b);
                    r.quad(x + PW - 17, rowY + (MH - 5) / 2, 5, 5, GuiRenderer.CIRCLE, new Color(dr, dg, db, 255));

                    if (hasSettings(entry.element.settings)) {
                        drawSettingsHandle(r, x + PW - 31, rowY + MH / 2, hA, overHandle(x, rowY));
                    }
                }
            }
            rowY += MH;
        }

        r.scissorEnd();
    }

    private void drawThemePanel(GuiRenderer r, GuiGraphicsExtractor gfx, Panel p, float delta, float fade) {
        drawPanelFrame(r, gfx, p, p.bodyH, fade);
        if (p.openAnim < 0.02f) return;

        double x = p.x, y = p.y;
        double visBody = p.bodyH * p.openAnim;
        double rowY = y + HH, clipB = y + HH + visBody;

        r.scissorStart(0, 0, getWindowWidth(), getWindowHeight());

        for (int i = 0; i < EmberPalette.NAMES.length; i++) {
            if (rowY + MH <= clipB + 0.5) {
                boolean hover = mx >= x && mx < x + PW && my >= rowY && my < rowY + MH;
                boolean sel = i == EmberPalette.selected();
                float hA = anim("th_" + i, hoverAnims, hover ? 1f : 0f, 12f, frameDt);

                if (sel) activeGlow(r, x + 4, rowY + 1, PW - 8, MH - 2, 1f);
                if (sel) r.roundedRect(x + 4, rowY + 1, PW - 8, MH - 2, (MH - 2) / 2, activeRowColor(1f));
                if (hA > 0.01f) rowHover(r, x + 4, rowY + 1, PW - 8, MH - 2, hA, sel);

                r.quad(x + 11, rowY + (MH - 8) / 2, 8, 8, GuiRenderer.CIRCLE, EmberPalette.swatch(i));

                if (sel) {
                    r.quad(x + PW - 17, rowY + (MH - 5) / 2, 5, 5, GuiRenderer.CIRCLE, TEXT_WHITE);
                }
            }
            rowY += MH;
        }

        r.scissorEnd();
    }

    // --- Text ---

    private void drawPanelText(GuiGraphicsExtractor gfx, Panel p) {
        double x = p.x, y = p.y;
        List<Module> mods = filtered(p);

        theme.textRenderer().begin(gfx, theme.scale(0.9));
        theme.textRenderer().render(p.name, x + 26, y + (HH - theme.textHeight()) / 2, HEADER_TEXT, false);
        theme.textRenderer().end();

        if (p.openAnim < 0.02f) return;

        double visBody = p.bodyH * p.openAnim;
        theme.textRenderer().begin(gfx, theme.scale(0.8));
        double rowY = y + HH - p.scroll;
        double clipT = y + HH, clipB = y + HH + visBody;

        for (Module m : mods) {
            if (rowY >= clipT - 0.5 && rowY + MH <= clipB + 0.5) {
                float aA = activeAnims.getOrDefault((Object) m, 0f);
                int cr = (int) Mth.lerp(aA, TEXT_INACTIVE.r, ACCENT.r);
                int cg = (int) Mth.lerp(aA, TEXT_INACTIVE.g, ACCENT.g);
                int cb = (int) Mth.lerp(aA, TEXT_INACTIVE.b, ACCENT.b);
                theme.textRenderer().render(m.title, x + 14,
                    rowY + (MH - theme.textHeight()) / 2, new Color(cr, cg, cb, 255), false);
            }
            rowY += MH;
        }
        theme.textRenderer().end();
    }

    private void drawClientPanelText(GuiGraphicsExtractor gfx, Panel p) {
        double x = p.x, y = p.y;
        theme.textRenderer().begin(gfx, theme.scale(0.9));
        theme.textRenderer().render("Client", x + 26, y + (HH - theme.textHeight()) / 2, HEADER_TEXT, false);
        theme.textRenderer().end();

        if (p.openAnim < 0.02f || p.clientEntries == null) return;

        double visBody = p.bodyH * p.openAnim;
        theme.textRenderer().begin(gfx, theme.scale(0.8));
        double rowY = y + HH - p.scroll;
        double clipT = y + HH, clipB = y + HH + visBody;

        for (ClientEntry entry : p.clientEntries) {
            if (rowY >= clipT - 0.5 && rowY + MH <= clipB + 0.5) {
                Color tc;
                if (entry.element == null) {
                    tc = TEXT_WHITE;
                } else {
                    float aA = activeAnims.getOrDefault((Object)("cl_" + entry.name), 0f);
                    tc = new Color(
                        (int) Mth.lerp(aA, TEXT_INACTIVE.r, ACCENT.r),
                        (int) Mth.lerp(aA, TEXT_INACTIVE.g, ACCENT.g),
                        (int) Mth.lerp(aA, TEXT_INACTIVE.b, ACCENT.b), 255);
                }
                theme.textRenderer().render(entry.name, x + 14,
                    rowY + (MH - theme.textHeight()) / 2, tc, false);
            }
            rowY += MH;
        }
        theme.textRenderer().end();
    }

    private void drawThemePanelText(GuiGraphicsExtractor gfx, Panel p) {
        double x = p.x, y = p.y;
        theme.textRenderer().begin(gfx, theme.scale(0.9));
        theme.textRenderer().render("Themes", x + 26, y + (HH - theme.textHeight()) / 2, HEADER_TEXT, false);
        theme.textRenderer().end();

        if (p.openAnim < 0.02f) return;

        double visBody = p.bodyH * p.openAnim;
        theme.textRenderer().begin(gfx, theme.scale(0.8));
        double rowY = y + HH, clipB = y + HH + visBody;
        for (int i = 0; i < EmberPalette.NAMES.length; i++) {
            if (rowY + MH <= clipB + 0.5) {
                Color tc = i == EmberPalette.selected() ? TEXT_WHITE : TEXT_INACTIVE;
                theme.textRenderer().render(EmberPalette.NAMES[i], x + 30,
                    rowY + (MH - theme.textHeight()) / 2, tc, false);
            }
            rowY += MH;
        }
        theme.textRenderer().end();
    }

    private void drawSearchBar(GuiRenderer r, float fade) {
        double w = 220, h = 26;
        double x = (getWindowWidth() - w) / 2, y = 8;
        if (searchFocused) r.glow(x, y, w, h, 10, accentAlpha((int)(120 * fade)), false);
        r.roundedRect(x, y, w, h, h / 2, new Color(SEARCH_BG.r, SEARCH_BG.g, SEARCH_BG.b, (int)(SEARCH_BG.a * fade)));
    }

    private void drawSearchText(GuiGraphicsExtractor gfx) {
        double w = 220, h = 26;
        double x = (getWindowWidth() - w) / 2, y = 8;
        theme.textRenderer().begin(gfx, theme.scale(0.78));
        if (search.isEmpty() && !searchFocused) {
            theme.textRenderer().render("Search...", x + 14,
                y + (h - theme.textHeight()) / 2, TEXT_FAINT, false);
        } else {
            theme.textRenderer().render(search + (searchFocused ? "|" : ""), x + 14,
                y + (h - theme.textHeight()) / 2, TEXT_WHITE, false);
        }
        theme.textRenderer().end();
    }

    // --- Helpers ---

    private List<Module> filtered(Panel p) {
        if (p.modules == null) return Collections.emptyList();
        if (search.isEmpty()) return p.modules;
        String q = search.toLowerCase();
        List<Module> out = new ArrayList<>();
        for (Module m : p.modules) if (m.title.toLowerCase().contains(q) || m.name.toLowerCase().contains(q)) out.add(m);
        return out;
    }

    private static boolean hasSettings(Settings settings) {
        for (SettingGroup g : settings) for (Setting<?> s : g) if (s.isVisible()) return true;
        return false;
    }

    /** Opens the settings popup for a module, e.g. from the .settings command. */
    public void openSettingsFor(Module module) {
        openModuleSettings(module);
    }

    private void openModuleSettings(Module m) {
        popup.open(new EmberSettingsPopup.Target(
            m.title, m.description, m.settings, m::isActive, m::toggle, m.keybind,
            () -> mc.gui.setScreen(new ModuleScreen(theme, m))
        ));
    }

    private void openHudSettings(HudElement element) {
        popup.open(new EmberSettingsPopup.Target(
            element.info.title, element.info.description, element.settings, element::isActive, element::toggle, null,
            () -> mc.gui.setScreen(new HudElementScreen(theme, element))
        ));
    }

    // --- Input ---

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double s = mc.getWindow().getGuiScale();
        double cx = click.x() * s, cy = click.y() * s;
        int btn = click.button();

        if (popup.isVisible()) return popup.mouseClicked(cx, cy, btn);

        double sw = 220, sh = 26;
        double sx = (getWindowWidth() - sw) / 2, sy = 8;
        if (cx >= sx && cx < sx + sw && cy >= sy && cy < sy + sh) { searchFocused = true; return true; }
        else searchFocused = false;

        double bx = configButtonX(), by = configButtonY();
        if (cx >= bx && cx < bx + CFG_W && cy >= by && cy < by + CFG_H) {
            mc.gui.setScreen(new EmberConfigScreen(theme));
            return true;
        }

        double gx = gearX(), gy = gearY();
        if (cx >= gx && cx < gx + GEAR && cy >= gy && cy < gy + GEAR) {
            mc.gui.setScreen(new EmberClientSettingsScreen(theme));
            return true;
        }

        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);

            if (cx >= p.x && cx < p.x + PW && cy >= p.y && cy < p.y + HH) {
                if (btn == 1) { p.collapsed = !p.collapsed; return true; }
                dragging = p; dragOX = cx - p.x; dragOY = cy - p.y;
                panels.remove(i); panels.add(p);
                return true;
            }

            if (p.openAnim < 0.1f) continue;
            double visBody = p.bodyH * p.openAnim;
            double totalH = HH + visBody;
            if (cx < p.x || cx >= p.x + PW || cy < p.y || cy >= p.y + totalH) continue;

            if (p.isTheme) {
                double rowY = p.y + HH;
                for (int ti = 0; ti < EmberPalette.NAMES.length; ti++) {
                    if (cy >= rowY && cy < rowY + MH) { EmberPalette.select(ti); return true; }
                    rowY += MH;
                }
                continue;
            }

            boolean onHandle = cx >= p.x + PW - HANDLE_W;
            double rowY = p.y + HH - p.scroll;

            if (p.isClient && p.clientEntries != null) {
                for (ClientEntry entry : p.clientEntries) {
                    if (cy >= rowY && cy < rowY + MH) {
                        if (entry.element == null) {
                            if (btn == 0 && entry.action != null) entry.action.run();
                        } else if (btn == 1 || (onHandle && hasSettings(entry.element.settings))) {
                            openHudSettings(entry.element);
                        } else if (btn == 0) {
                            entry.element.toggle();
                        }
                        return true;
                    }
                    rowY += MH;
                }
                continue;
            }

            for (Module m : filtered(p)) {
                if (cy >= rowY && cy < rowY + MH) {
                    // Right-click always opens settings, even for modules that only have a bind.
                    if (btn == 1 || (onHandle && hasSettings(m.settings))) openModuleSettings(m);
                    else if (btn == 0) m.toggle();
                    return true;
                }
                rowY += MH;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        popup.mouseReleased();

        if (dragging != null) {
            saved.put(dragging.name, new double[]{dragging.x, dragging.y, dragging.collapsed ? 1 : 0});
            dragging = null;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        if (popup.isVisible()) return popup.mouseScrolled(v);

        double s = mc.getWindow().getGuiScale();
        double sx = mouseX * s, sy = mouseY * s;
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            double totalH = HH + p.bodyH * p.openAnim;
            if (sx >= p.x && sx < p.x + PW && sy >= p.y && sy < p.y + totalH) {
                p.scroll -= (int)(v * MH);
                p.scroll = Math.max(0, p.scroll);

                double maxBody;
                if (p.isClient && p.clientEntries != null) maxBody = p.clientEntries.size() * MH;
                else if (!p.isTheme && p.modules != null) maxBody = filtered(p).size() * MH;
                else maxBody = 0;

                p.scroll = Math.min(p.scroll, (int)Math.max(0, maxBody - bodyLimit(p)));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        // Esc closes the popup first instead of the whole GUI.
        if (popup.isVisible()) return popup.keyPressed(input);

        if (searchFocused) {
            if (input.key() == GLFW_KEY_ESCAPE) { searchFocused = false; search = ""; return true; }
            if (input.key() == GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1); return true;
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (popup.isVisible()) return popup.charTyped(input);

        if (searchFocused) {
            char c = (char) input.codepoint();
            if (c >= 32) search += c;
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void onClose() {
        for (Panel p : panels) saved.put(p.name, new double[]{p.x, p.y, p.collapsed ? 1 : 0});
        super.onClose();
    }

    @Override public void reload() {}

    private static class Panel {
        Category category; String name; List<Module> modules; ItemStack icon;
        double x, y, bodyH; boolean collapsed; float openAnim = 1f; int scroll;
        boolean isClient, isTheme; List<ClientEntry> clientEntries;
    }

    private static class ClientEntry {
        String name; HudElement element; Runnable action;

        ClientEntry(String n, HudElement e) { name = n; element = e; }

        ClientEntry(String n, Runnable a) { name = n; action = a; }
    }
}
