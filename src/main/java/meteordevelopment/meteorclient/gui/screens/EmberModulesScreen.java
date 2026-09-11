package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class EmberModulesScreen extends TabScreen {
    private static final Color BG_OVERLAY = new Color(0, 0, 0, 200);
    private static final Color PANEL_BG = new Color(12, 12, 14, 255);
    private static final Color SIDEBAR_BG = new Color(8, 8, 10, 255);
    private static final Color CARD_BG = new Color(20, 20, 24, 255);
    private static final Color CARD_HOVER = new Color(28, 28, 34, 255);
    private static final Color ACCENT = new Color(255, 107, 53, 255);
    private static final Color TEXT_WHITE = new Color(255, 255, 255, 255);
    private static final Color TEXT_DIM = new Color(140, 140, 155, 255);
    private static final Color TOGGLE_OFF = new Color(42, 42, 50, 255);
    private static final Color DIVIDER = new Color(30, 30, 36, 255);

    private final List<Category> categories = new ArrayList<>();
    private Category selected;
    private int scroll = 0;
    private double mx, my;

    private final Map<Module, Float> toggleAnims = new HashMap<>();
    private final Map<Category, Float> catAnims = new HashMap<>();
    private float openAnim = 0;

    private double px, py, pw, ph;
    private static final double SIDEBAR_W = 58;
    private static final double CARD_H = 44;
    private static final double CARD_GAP = 6;
    private static final double PAD = 16;
    private static final double CORNER_R = 12;
    private double cardW;

    public EmberModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
        for (Category c : Modules.loopCategories()) {
            if (!Modules.get().getGroup(c).isEmpty()) categories.add(c);
        }
        if (!categories.isEmpty()) selected = categories.get(0);
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

        pw = Math.min(820, getWindowWidth() - 60);
        ph = Math.min(520, getWindowHeight() - 50);
        px = (getWindowWidth() - pw) / 2;
        py = (getWindowHeight() - ph) / 2;
        cardW = (pw - SIDEBAR_W - PAD * 3 - CARD_GAP) / 2;

        GuiRenderer r = new GuiRenderer();
        r.theme = theme;
        r.begin(graphics);

        // Dark overlay
        r.quad(0, 0, getWindowWidth(), getWindowHeight(), new Color(0, 0, 0, (int) (200 * ease)));

        // Shadow layers
        for (int i = 24; i > 0; i -= 4) {
            float a = (float) i / 24f;
            r.quad(px - i, py - i, pw + i * 2, ph + i * 2, new Color(0, 0, 0, (int) (50 * a * ease)));
        }

        // Main panel
        r.roundedRect(px, py, pw, ph, CORNER_R, PANEL_BG);

        // Sidebar
        r.roundedRect(px, py, SIDEBAR_W, ph, CORNER_R, SIDEBAR_BG);
        r.quad(px + SIDEBAR_W - CORNER_R, py, CORNER_R, ph, SIDEBAR_BG);

        // Top accent bar across content area
        double barX = px + SIDEBAR_W;
        double barW = pw - SIDEBAR_W;
        r.quad(barX, py, barW, 3, ACCENT);
        Color glowTop = new Color(ACCENT.r, ACCENT.g, ACCENT.b, (int) (40 * ease));
        Color glowBot = new Color(ACCENT.r, ACCENT.g, ACCENT.b, 0);
        r.quad(barX, py + 3, barW, 18, glowTop, glowTop, glowBot, glowBot);

        // Sidebar category highlights and selection indicator
        double catH = 48;
        double catStartY = py + 16;
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            double cy = catStartY + i * catH;
            boolean hover = mx >= px && mx < px + SIDEBAR_W && my >= cy && my < cy + catH;
            boolean sel = cat == selected;

            float target = sel ? 1f : (hover ? 0.5f : 0f);
            float cur = catAnims.getOrDefault(cat, 0f);
            cur = Mth.lerp(delta * 0.2f, cur, target);
            catAnims.put(cat, cur);

            if (cur > 0.01f) {
                r.roundedRect(px + 4, cy + 6, SIDEBAR_W - 8, catH - 12, 8,
                    new Color(ACCENT.r, ACCENT.g, ACCENT.b, (int) (30 * cur)));
                r.roundedRect(px + 2, cy + 12, 3, catH - 24, 1.5,
                    new Color(ACCENT.r, ACCENT.g, ACCENT.b, (int) (255 * cur)));
            }
        }

        // Divider between sidebar and content
        r.quad(px + SIDEBAR_W, py + 3, 1, ph - 3, DIVIDER);

        // Module cards
        if (selected != null) {
            List<Module> mods = Modules.get().getGroup(selected);
            double contentX = px + SIDEBAR_W + PAD;
            double headerY = py + PAD + 8;

            double startY = headerY + 36 - scroll;
            int col = 0;
            double rowY = startY;
            double clipTop = py + 42;
            double clipBot = py + ph - 8;

            for (Module mod : mods) {
                double cx = contentX + col * (cardW + CARD_GAP);
                double cy = rowY;

                if (cy + CARD_H > clipTop && cy < clipBot) {
                    boolean hover = mx >= cx && mx < cx + cardW && my >= cy && my < cy + CARD_H;

                    float target = mod.isActive() ? 1f : 0f;
                    float cur = toggleAnims.getOrDefault(mod, 0f);
                    cur = Mth.lerp(delta * 0.15f, cur, target);
                    toggleAnims.put(mod, cur);

                    // Card background
                    Color cardCol = hover ? CARD_HOVER : CARD_BG;
                    if (cur > 0.1f) {
                        cardCol = new Color(
                            (int) Mth.lerp(cur * 0.3f, cardCol.r, ACCENT.r),
                            (int) Mth.lerp(cur * 0.15f, cardCol.g, ACCENT.g),
                            (int) Mth.lerp(cur * 0.1f, cardCol.b, ACCENT.b), 255);
                    }
                    r.roundedRect(cx, cy, cardW, CARD_H, 8, cardCol);

                    // Active accent bar on left
                    if (cur > 0.01f) {
                        r.roundedRect(cx + 5, cy + 10, 3, CARD_H - 20, 1.5,
                            new Color(ACCENT.r, ACCENT.g, ACCENT.b, (int) (255 * cur)));
                    }

                    // Toggle switch (pill shape)
                    double tW = 38, tH = 20;
                    double tX = cx + cardW - tW - 12;
                    double tY = cy + (CARD_H - tH) / 2;

                    Color trackCol = new Color(
                        (int) Mth.lerp(cur, TOGGLE_OFF.r, ACCENT.r),
                        (int) Mth.lerp(cur, TOGGLE_OFF.g, ACCENT.g),
                        (int) Mth.lerp(cur, TOGGLE_OFF.b, ACCENT.b), 255);
                    r.roundedRect(tX, tY, tW, tH, tH / 2, trackCol);

                    // Toggle knob
                    double knobD = tH - 4;
                    double knobX = tX + 2 + (tW - knobD - 4) * cur;
                    r.quad(knobX, tY + 2, knobD, knobD, GuiRenderer.CIRCLE, TEXT_WHITE);
                }

                col++;
                if (col >= 2) { col = 0; rowY += CARD_H + CARD_GAP; }
            }
        }

        // Item icons rendered via post() so they appear on top of quads
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            double cy = catStartY + i * catH;
            ItemStack icon = cat.icon.get();
            int ix = (int) (px + (SIDEBAR_W - 32) / 2);
            int iy = (int) (cy + (catH - 32) / 2);
            final int fx = ix, fy = iy;
            final ItemStack fIcon = icon;
            r.post(() -> RenderUtils.drawItem(graphics, fIcon, fx, fy, 2.0f, false, null, false));
        }

        r.end();

        // Category title text
        if (selected != null) {
            double contentX = px + SIDEBAR_W + PAD;
            double headerY = py + PAD + 8;

            theme.textRenderer().begin(graphics, theme.scale(1.4));
            theme.textRenderer().render(selected.name, contentX, headerY, TEXT_WHITE, false);
            theme.textRenderer().end();

            // Module count
            int count = Modules.get().getGroup(selected).size();
            theme.textRenderer().begin(graphics, theme.scale(0.85));
            theme.textRenderer().render(count + " modules", contentX + theme.textWidth(selected.name) * 1.4 + 12, headerY + 4, TEXT_DIM, false);
            theme.textRenderer().end();

            // Module name labels
            List<Module> mods = Modules.get().getGroup(selected);
            double startY = headerY + 36 - scroll;
            double clipTop = py + 42;
            double clipBot = py + ph - 8;

            theme.textRenderer().begin(graphics, theme.scale(1.0));
            int col = 0;
            double rowY = startY;
            for (Module mod : mods) {
                double cx = contentX + col * (cardW + CARD_GAP);
                double cy = rowY;

                if (cy + CARD_H > clipTop && cy < clipBot) {
                    float a = toggleAnims.getOrDefault(mod, 0f);
                    Color tc = a > 0.5f ? TEXT_WHITE : new Color(200, 200, 210, 255);
                    theme.textRenderer().render(mod.title, cx + 14, cy + (CARD_H - theme.textHeight()) / 2, tc, false);
                }

                col++;
                if (col >= 2) { col = 0; rowY += CARD_H + CARD_GAP; }
            }
            theme.textRenderer().end();
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        double s = mc.getWindow().getGuiScale();
        double clickX = click.x() * s;
        double clickY = click.y() * s;

        // Sidebar category clicks
        double catH = 48;
        double catStartY = py + 16;
        for (int i = 0; i < categories.size(); i++) {
            double cy = catStartY + i * catH;
            if (clickX >= px && clickX < px + SIDEBAR_W && clickY >= cy && clickY < cy + catH) {
                selected = categories.get(i);
                scroll = 0;
                return true;
            }
        }

        // Module card clicks
        if (selected != null) {
            List<Module> mods = Modules.get().getGroup(selected);
            double contentX = px + SIDEBAR_W + PAD;
            double startY = py + PAD + 8 + 36 - scroll;

            int col = 0;
            double rowY = startY;
            for (Module mod : mods) {
                double cx = contentX + col * (cardW + CARD_GAP);
                double cy = rowY;

                if (clickX >= cx && clickX < cx + cardW && clickY >= cy && clickY < cy + CARD_H) {
                    if (click.button() == 0) mod.toggle();
                    else if (click.button() == 1) mc.gui.setScreen(theme.moduleScreen(mod));
                    return true;
                }

                col++;
                if (col >= 2) { col = 0; rowY += CARD_H + CARD_GAP; }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        scroll -= (int) (v * 30);
        scroll = Math.max(0, scroll);
        if (selected != null) {
            int rows = (Modules.get().getGroup(selected).size() + 1) / 2;
            int maxScroll = (int) Math.max(0, rows * (CARD_H + CARD_GAP) - (ph - 80));
            scroll = Math.min(scroll, maxScroll);
        }
        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    @Override
    public void reload() {}
}
