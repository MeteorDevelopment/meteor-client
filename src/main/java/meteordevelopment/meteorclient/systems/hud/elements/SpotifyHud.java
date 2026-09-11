package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.misc.MediaInfo;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.EmberPalette;

public class SpotifyHud extends HudElement {
    public static final HudElementInfo<SpotifyHud> INFO = new HudElementInfo<>(Hud.GROUP, "spotify", "Shows the song currently playing on your PC.", SpotifyHud::new);

    /** Album art must be tinted pure white or it renders off-colour. */
    private static final Color ART_TINT = new Color(255, 255, 255, 255);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

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

    public SpotifyHud() {
        super(INFO);
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
        double titleScale = 1.05 * s;
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

        renderer.glow(x, y + 2 * s, w, h, 6 * s, new Color(0, 0, 0, 90));
        if (glow.get()) renderer.glow(x, y, w, h, 10 * s, new Color(accent.r, accent.g, accent.b, 120));

        renderer.roundedQuad(x, y, w, h, radius, bg);

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
            renderer.text(truncate(renderer, artist, contentW, subScale), cx, ty, gray, true, subScale);
        }

        if (withProgress) {
            ty += (withArtist ? subH : titleH) + rowGap;

            String elapsed = MediaInfo.formatTime(pos);
            String total = MediaInfo.formatTime(dur);

            double elapsedW = renderer.textWidth(elapsed, true, subScale);
            double totalW = renderer.textWidth(total, true, subScale);

            renderer.text(elapsed, cx, ty, gray, true, subScale);
            renderer.text(total, cx + contentW - totalW, ty, gray, true, subScale);

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
