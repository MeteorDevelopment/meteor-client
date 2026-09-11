package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.systems.profiles.Profile;
import meteordevelopment.meteorclient.systems.profiles.Profiles;
import meteordevelopment.meteorclient.utils.render.EmberAnim;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.EmberPalette;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static org.lwjgl.glfw.GLFW.*;

public class EmberConfigScreen extends WidgetScreen {
    private static final Color DANGER = new Color(222, 86, 86, 255);

    // Refreshed from EmberPalette every frame, so this screen follows the selected theme.
    private static Color PANEL_BG, HEADER_BG, DIVIDER, ROW_BG, INPUT_BG, LIST_BG, DISABLED, TEXT_WHITE, TEXT_DIM, ON_ACCENT;

    static {
        syncPalette();
    }

    private static final double PW = 420;
    private static final double PH = 380;
    private static final double PR = 10;
    private static final double ROW_H = 34;

    private double px, py;
    private double mx, my;

    private String name = "";
    private boolean nameFocused = true;
    private int scroll = 0;
    private float fade = 0f;
    private String status = "";
    private long statusUntil = 0;

    private final Map<Object, Float> hoverAnims = new HashMap<>();
    private final EmberAnim.Clock clock = new EmberAnim.Clock();

    public EmberConfigScreen(GuiTheme theme) {
        super(theme, "Configs");
    }

    private static void syncPalette() {
        Color panel = EmberPalette.panel();
        PANEL_BG = new Color(panel.r, panel.g, panel.b, 248);
        HEADER_BG = EmberPalette.header();
        DIVIDER = EmberPalette.divider();
        ROW_BG = EmberPalette.control();
        INPUT_BG = new Color(panel.r, panel.g, panel.b, 255);
        LIST_BG = new Color(EmberPalette.search().r, EmberPalette.search().g, EmberPalette.search().b, 200);
        DISABLED = EmberPalette.toggleOff();
        TEXT_WHITE = EmberPalette.textBright();
        TEXT_DIM = EmberPalette.textDim();
        // Dark text for use on top of accent-coloured buttons.
        ON_ACCENT = new Color(panel.r, panel.g, panel.b, 255);
    }

    @Override
    public void initWidgets() {
        clear();
    }

    private Color accent() {
        return EmberPalette.accent();
    }

    private Color accentAlpha(int a) {
        Color c = accent();
        return new Color(c.r, c.g, c.b, Math.max(0, Math.min(255, a)));
    }

    /** Real-time easing: settles in about a fifth of a second regardless of frame rate. */
    private float anim(Object key, float target, float dt) {
        float cur = EmberAnim.approach(hoverAnims.getOrDefault(key, 0f), target, dt, 0.065);
        hoverAnims.put(key, cur);
        return cur;
    }

    private List<Profile> profiles() {
        return new ArrayList<>(Profiles.get().getAll());
    }

    @Override
    protected void onRenderBefore(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        syncPalette();
        float dt = clock.tick();

        mx = mouseX;
        my = mouseY;

        fade = Math.min(1f, fade + dt / 0.25f);
        float f = 1f - (1f - fade) * (1f - fade) * (1f - fade);

        px = (getWindowWidth() - PW) / 2;
        py = (getWindowHeight() - PH) / 2;

        GuiRenderer r = new GuiRenderer();
        r.theme = theme;
        r.begin(graphics);

        // Dim background in its own batch so it doesn't darken the glow.
        r.scissorStart(0, 0, getWindowWidth(), getWindowHeight());
        r.quad(0, 0, getWindowWidth(), getWindowHeight(), new Color(0, 0, 0, (int) (150 * f)));
        r.scissorEnd();

        r.glow(px, py + 8, PW, PH, 24, new Color(0, 0, 0, (int) (130 * f)), false);
        r.glow(px, py, PW, PH, 34, accentAlpha((int) (160 * f)), false);
        r.roundedRect(px, py, PW, PH, PR, PANEL_BG);

        // Header
        double hh = 40;
        r.roundedRect(px, py, PW, hh + PR, PR, HEADER_BG);
        r.quad(px, py + hh, PW, PR, HEADER_BG);
        r.quad(px, py + hh, PW, 1, DIVIDER);

        // Close button
        double cbX = px + PW - 32, cbY = py + 12;
        boolean closeHover = mx >= cbX - 4 && mx < cbX + 20 && my >= cbY - 4 && my < cbY + 20;
        float ca = anim("close", closeHover ? 1f : 0f, dt);
        if (ca > 0.01f) r.roundedRect(cbX - 6, cbY - 5, 26, 26, 6, new Color(DANGER.r, DANGER.g, DANGER.b, (int) (60 * ca)));

        // Name input
        double inX = px + 16, inY = py + hh + 16;
        double inW = PW - 32 - 96, inH = 30;
        if (nameFocused) r.glow(inX, inY, inW, inH, 8, accentAlpha(120), false);
        r.roundedRect(inX, inY, inW, inH, 7, INPUT_BG);

        // Save button
        double sbX = px + PW - 16 - 88, sbY = inY;
        double sbW = 88, sbH = inH;
        boolean saveHover = mx >= sbX && mx < sbX + sbW && my >= sbY && my < sbY + sbH;
        float sa = anim("save", saveHover ? 1f : 0f, dt);
        boolean canSave = !name.trim().isEmpty();

        if (canSave && sa > 0.01f) r.glow(sbX, sbY, sbW, sbH, 12, accentAlpha((int) (130 * sa)), false);

        Color sb = canSave
            ? new Color(accent().r, accent().g, accent().b, (int) (200 + 55 * sa))
            : DISABLED;
        r.roundedRect(sbX, sbY, sbW, sbH, 7, sb);

        // List
        double listY = inY + inH + 14;
        double listH = py + PH - 16 - listY;
        r.roundedRect(px + 16, listY, PW - 32, listH, 8, LIST_BG);

        List<Profile> list = profiles();
        double rowY = listY + 6 - scroll;

        for (Profile p : list) {
            if (rowY + ROW_H > listY && rowY < listY + listH) {
                boolean rowHover = mx >= px + 22 && mx < px + PW - 22 && my >= rowY && my < rowY + ROW_H
                    && my >= listY && my < listY + listH;
                float ra = anim("row_" + p.name.get(), rowHover ? 1f : 0f, dt);

                r.roundedRect(px + 22, rowY, PW - 44, ROW_H - 4, 7,
                    new Color(
                        (int) Mth.lerp(ra * 0.25f, ROW_BG.r, accent().r),
                        (int) Mth.lerp(ra * 0.25f, ROW_BG.g, accent().g),
                        (int) Mth.lerp(ra * 0.25f, ROW_BG.b, accent().b), 235));

                // Load button
                double lbX = px + PW - 44 - 110, lbY = rowY + 5;
                boolean lbHover = mx >= lbX && mx < lbX + 62 && my >= lbY && my < lbY + ROW_H - 14;
                float la = anim("load_" + p.name.get(), lbHover ? 1f : 0f, dt);
                r.roundedRect(lbX, lbY, 62, ROW_H - 14, 6,
                    new Color(accent().r, accent().g, accent().b, (int) (150 + 90 * la)));

                // Delete button
                double dbX = px + PW - 44 - 40, dbY = rowY + 5;
                boolean dbHover = mx >= dbX && mx < dbX + 34 && my >= dbY && my < dbY + ROW_H - 14;
                float da = anim("del_" + p.name.get(), dbHover ? 1f : 0f, dt);
                r.roundedRect(dbX, dbY, 34, ROW_H - 14, 6,
                    new Color(DANGER.r, DANGER.g, DANGER.b, (int) (90 + 110 * da)));
            }
            rowY += ROW_H;
        }

        r.end();

        // --- Text ---
        theme.textRenderer().begin(graphics, theme.scale(1.1));
        theme.textRenderer().render("Configs", px + 18, py + (hh - theme.textHeight()) / 2, TEXT_WHITE, false);
        theme.textRenderer().end();

        theme.textRenderer().begin(graphics, theme.scale(1.0));
        theme.textRenderer().render("X", cbX + 4, cbY + 4,
            closeHover ? new Color(255, 140, 140, 255) : TEXT_DIM, false);
        theme.textRenderer().end();

        theme.textRenderer().begin(graphics, theme.scale(0.95));
        String shown = name.isEmpty() && !nameFocused ? "Config name..." : name + (nameFocused ? "|" : "");
        theme.textRenderer().render(shown, inX + 10, inY + (inH - theme.textHeight()) / 2,
            name.isEmpty() && !nameFocused ? TEXT_DIM : TEXT_WHITE, false);

        double saveTw = theme.textWidth("Save");
        theme.textRenderer().render("Save", sbX + (sbW - saveTw) / 2, sbY + (sbH - theme.textHeight()) / 2,
            canSave ? ON_ACCENT : TEXT_DIM, false);
        theme.textRenderer().end();

        theme.textRenderer().begin(graphics, theme.scale(0.9));
        if (list.isEmpty()) {
            String empty = "No configs saved yet";
            theme.textRenderer().render(empty, px + (PW - theme.textWidth(empty)) / 2,
                listY + listH / 2 - theme.textHeight() / 2, TEXT_DIM, false);
        }

        rowY = listY + 6 - scroll;
        for (Profile p : list) {
            if (rowY + ROW_H > listY && rowY < listY + listH) {
                theme.textRenderer().render(p.name.get(), px + 32,
                    rowY + (ROW_H - 4 - theme.textHeight()) / 2, TEXT_WHITE, false);

                double lbX = px + PW - 44 - 110;
                double ltw = theme.textWidth("Load");
                theme.textRenderer().render("Load", lbX + (62 - ltw) / 2,
                    rowY + (ROW_H - 4 - theme.textHeight()) / 2, ON_ACCENT, false);

                double dbX = px + PW - 44 - 40;
                double dtw = theme.textWidth("Del");
                theme.textRenderer().render("Del", dbX + (34 - dtw) / 2,
                    rowY + (ROW_H - 4 - theme.textHeight()) / 2, TEXT_WHITE, false);
            }
            rowY += ROW_H;
        }
        theme.textRenderer().end();

        if (!status.isEmpty() && System.currentTimeMillis() < statusUntil) {
            theme.textRenderer().begin(graphics, theme.scale(0.9));
            theme.textRenderer().render(status, px + 18, py + PH + 8, accent(), false);
            theme.textRenderer().end();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double s = mc.getWindow().getGuiScale();
        double cx = click.x() * s, cy = click.y() * s;

        double hh = 40;

        // Close
        double cbX = px + PW - 32, cbY = py + 12;
        if (cx >= cbX - 6 && cx < cbX + 20 && cy >= cbY - 5 && cy < cbY + 21) {
            onClose();
            return true;
        }

        // Name input
        double inX = px + 16, inY = py + hh + 16;
        double inW = PW - 32 - 96, inH = 30;
        nameFocused = cx >= inX && cx < inX + inW && cy >= inY && cy < inY + inH;

        // Save
        double sbX = px + PW - 16 - 88, sbY = inY;
        if (cx >= sbX && cx < sbX + 88 && cy >= sbY && cy < sbY + inH) {
            saveConfig();
            return true;
        }

        // Rows
        double listY = inY + inH + 14;
        double listH = py + PH - 16 - listY;
        double rowY = listY + 6 - scroll;

        for (Profile p : profiles()) {
            if (cy >= rowY && cy < rowY + ROW_H && cy >= listY && cy < listY + listH) {
                double lbX = px + PW - 44 - 110;
                if (cx >= lbX && cx < lbX + 62) {
                    p.load();
                    setStatus("Loaded " + p.name.get());
                    return true;
                }

                double dbX = px + PW - 44 - 40;
                if (cx >= dbX && cx < dbX + 34) {
                    Profiles.get().remove(p);
                    setStatus("Deleted " + p.name.get());
                    return true;
                }

                name = p.name.get();
                nameFocused = false;
                return true;
            }
            rowY += ROW_H;
        }

        return super.mouseClicked(click, doubled);
    }

    private void saveConfig() {
        String n = name.trim();
        if (n.isEmpty()) return;

        Profile existing = Profiles.get().get(n);
        Profile p = existing != null ? existing : new Profile();

        p.name.set(n);
        p.hud.set(true);
        p.macros.set(true);
        p.modules.set(true);
        p.waypoints.set(true);

        if (existing != null) {
            p.save();
            Profiles.get().save();
        } else {
            Profiles.get().add(p);
        }

        setStatus("Saved " + n);
    }

    private void setStatus(String s) {
        status = s;
        statusUntil = System.currentTimeMillis() + 2500;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        scroll -= (int) (v * 24);
        scroll = Math.max(0, scroll);

        int max = (int) Math.max(0, profiles().size() * ROW_H - (PH - 150));
        scroll = Math.min(scroll, max);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        if (nameFocused) {
            if (input.key() == GLFW_KEY_BACKSPACE && !name.isEmpty()) {
                name = name.substring(0, name.length() - 1);
                return true;
            }
            if (input.key() == GLFW_KEY_ENTER || input.key() == GLFW_KEY_KP_ENTER) {
                saveConfig();
                return true;
            }
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        // Go back to the ClickGUI this was opened from instead of closing to the game.
        if (parent != null) mc.gui.setScreen(parent);
        else super.onClose();
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (nameFocused) {
            char c = (char) input.codepoint();
            // Keep it a usable folder name
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == ' ') {
                if (name.length() < 32) name += c;
            }
            return true;
        }
        return super.charTyped(input);
    }
}
