package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.MediaInfo;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.EmberPalette;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class SpotifyHud extends HudElement {
    public static final HudElementInfo<SpotifyHud> INFO = new HudElementInfo<>(Hud.GROUP, "spotify", "Shows the song currently playing on your PC.", SpotifyHud::new);

    /** Album art must be tinted pure white or it renders off-colour. */
    private static final Color ART_TINT = new Color(255, 255, 255, 255);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgControls = settings.createGroup("Controls");

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Size of the widget.")
        .defaultValue(1.0)
        .min(0.5)
        .max(3.0)
        .sliderRange(0.5, 3.0)
        .build()
    );

    private final Setting<Double> width = sgGeneral.add(new DoubleSetting.Builder()
        .name("width")
        .description("How much room the song text gets.")
        .defaultValue(150.0)
        .min(80)
        .max(320)
        .sliderRange(80, 320)
        .build()
    );

    private final Setting<Boolean> showArt = sgGeneral.add(new BoolSetting.Builder()
        .name("show-album-art")
        .description("Show the album cover.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showProgress = sgGeneral.add(new BoolSetting.Builder()
        .name("show-progress")
        .description("Show the progress bar and timestamps.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> glow = sgGeneral.add(new BoolSetting.Builder()
        .name("glow")
        .description("Soft glow behind the widget.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> hideWhenIdle = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-when-idle")
        .description("Hide the widget when nothing is playing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Keybind> playPauseKey = sgControls.add(new KeybindSetting.Builder()
        .name("play-pause-key")
        .description("Play or pause whatever is playing.")
        .defaultValue(Keybind.none())
        .action(() -> control("toggle"))
        .build()
    );

    private final Setting<Keybind> nextKey = sgControls.add(new KeybindSetting.Builder()
        .name("next-key")
        .description("Skip to the next track.")
        .defaultValue(Keybind.none())
        .action(() -> control("next"))
        .build()
    );

    private final Setting<Keybind> previousKey = sgControls.add(new KeybindSetting.Builder()
        .name("previous-key")
        .description("Go back to the previous track.")
        .defaultValue(Keybind.none())
        .action(() -> control("prev"))
        .build()
    );

    public SpotifyHud() {
        super(INFO);
    }

    /** Keybind settings on HUD elements fire even when the widget is off, and while typing in chat. */
    private void control(String command) {
        if (!isActive() || mc.gui.screen() != null) return;
        MediaInfo.sendCommand(command);
    }

    @Override
    public void render(HudRenderer renderer) {
        MediaInfo.start();
        MediaInfo.uploadPendingArt();

        boolean has = MediaInfo.isAvailable();

        if (!has && hideWhenIdle.get() && !isInEditor()) {
            setSize(0, 0);
            return;
        }

        // Colours come from the Ember theme so the widget recolours with the ClickGUI.
        Color accent = EmberPalette.accent();
        Color panel = EmberPalette.panel();
        Color bg = new Color(panel.r, panel.g, panel.b, 225);
        Color artBg = EmberPalette.control();
        Color white = EmberPalette.textBright();
        Color gray = EmberPalette.textDim();
        Color track = EmberPalette.track();

        // The custom font rasterises a real typeface at scale*18px, so any scale is
        // smooth - no snapping, unlike Minecraft's bitmap font.
        double s = scale.get();
        double titleScale = 0.92 * s;
        double subScale = 0.85 * s;

        double titleH = renderer.textHeight(true, titleScale);
        double subH = renderer.textHeight(true, subScale);

        boolean withArt = showArt.get();
        boolean withProgress = showProgress.get();

        String title = has ? MediaInfo.getTitle() : "Nothing playing";
        String artist = has ? MediaInfo.getArtist() : "";
        int pos = MediaInfo.getPosition();
        int dur = MediaInfo.getDuration();

        boolean withArtist = !artist.isEmpty();

        double pad = 6 * s;
        double gap = 7 * s;
        double rowGap = 2 * s;
        double contentW = width.get() * s;
        double radius = 7 * s;

        double textH = titleH
            + (withArtist ? subH + rowGap : 0)
            + (withProgress ? subH + rowGap : 0);

        double art = textH;
        double w = pad + (withArt ? art + gap : 0) + contentW + pad;
        double h = pad * 2 + textH;

        setSize(w, h);

        // Built from the same rounded quads as the box itself. The texture-based glow was
        // not reaching the HUD at all, so the shadow it drew never appeared; stacked quads
        // fading outwards use the one call that demonstrably renders here.
        for (int i = 6; i >= 1; i--) {
            double spread = i * 1.8 * s;
            renderer.roundedQuad(x - spread, y - spread + 2.5 * s, w + spread * 2, h + spread * 2,
                radius + spread, new Color(0, 0, 0, 30 - i * 3));
        }
        if (glow.get()) renderer.glow(x, y, w, h, 10 * s, new Color(accent.r, accent.g, accent.b, 120));

        renderer.roundedQuad(x, y, w, h, radius, bg);

        // A lit top edge is what actually sells depth on a dark background; the shadow
        // alone has nothing darker to fall against.
        renderer.quad(x + radius, y, w - radius * 2, Math.max(1, 0.9 * s), new Color(255, 255, 255, 30));

        double cx = x + pad;

        if (withArt) {
            double ax = x + pad;
            double ay = y + pad;
            renderer.roundedQuad(ax, ay, art, art, 4 * s, artBg);

            if (MediaInfo.hasArt() && has) {
                renderer.post(() -> renderer.texture(MediaInfo.ART_ID, ax, ay, art, art, ART_TINT));
            } else {
                double mx = ax + art / 2, my = ay + art / 2;
                double n = art / 30;
                renderer.roundedQuad(mx - 5 * n, my + 1 * n, 5 * n, 4 * n, 2 * n, gray);
                renderer.quad(mx - 1 * n, my - 6 * n, 1.5 * n, 9 * n, gray);
                renderer.quad(mx - 1 * n, my - 6 * n, 6 * n, 1.5 * n, gray);
            }

            cx += art + gap;
        }

        double ty = y + pad;

        renderer.text(truncate(renderer, title, contentW, titleScale), cx, ty,
            has ? white : gray, true, titleScale);

        if (withArtist) {
            ty += titleH + rowGap;
            // No per-glyph shadow: at this size it thickens thin strokes and reads as fuzz.
            // The box already casts its own shadow, so the artist line stays crisp instead.
            Color artistColor = new Color(
                (gray.r * 2 + white.r) / 3,
                (gray.g * 2 + white.g) / 3,
                (gray.b * 2 + white.b) / 3,
                235);
            renderer.text(truncate(renderer, artist, contentW, subScale), cx, ty, artistColor, false, subScale);
        }

        if (withProgress) {
            ty += (withArtist ? subH : titleH) + rowGap;

            String elapsed = MediaInfo.formatTime(pos);
            String total = MediaInfo.formatTime(dur);

            double elapsedW = renderer.textWidth(elapsed, true, subScale);
            double totalW = renderer.textWidth(total, true, subScale);

            renderer.text(elapsed, cx, ty, gray, false, subScale);
            renderer.text(total, cx + contentW - totalW, ty, gray, false, subScale);

            double barX = cx + elapsedW + 5 * s;
            double barW = contentW - elapsedW - totalW - 10 * s;
            double barH = 3 * s;
            double barY = ty + subH / 2 - barH / 2;

            if (barW > 4 * s) {
                renderer.roundedQuad(barX, barY, barW, barH, barH / 2, track);

                double pct = dur > 0 ? Math.min(1.0, (double) pos / dur) : 0;
                double fill = barW * pct;
                if (fill > barH) renderer.roundedQuad(barX, barY, fill, barH, barH / 2, accent);

                if (has && dur > 0) {
                    double knob = 5 * s;
                    renderer.roundedQuad(barX + fill - knob / 2, barY - (knob - barH) / 2,
                        knob, knob, knob / 2, white);
                }
            }
        }
    }

    private String truncate(HudRenderer renderer, String s, double maxW, double textScale) {
        if (s.isEmpty()) return s;
        if (renderer.textWidth(s, true, textScale) <= maxW) return s;

        StringBuilder sb = new StringBuilder(s);
        while (sb.length() > 1 && renderer.textWidth(sb + "...", true, textScale) > maxW) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb + "...";
    }
}
