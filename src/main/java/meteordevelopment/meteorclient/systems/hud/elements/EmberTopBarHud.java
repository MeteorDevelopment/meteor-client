package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.EmberPalette;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EmberTopBarHud extends HudElement {
    public static final HudElementInfo<EmberTopBarHud> INFO = new HudElementInfo<>(Hud.GROUP, "ember-top-bar", "Ember Client top bar: name, user, ping, fps, server.", EmberTopBarHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Size of the bar.")
        .defaultValue(1.0)
        .min(0.5)
        .max(3.0)
        .sliderRange(0.5, 3.0)
        .build()
    );

    private final Setting<Boolean> showServer = sgGeneral.add(new BoolSetting.Builder()
        .name("show-server")
        .description("Show the server IP you are connected to.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showPing = sgGeneral.add(new BoolSetting.Builder()
        .name("show-ping")
        .description("Show your ping.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showFps = sgGeneral.add(new BoolSetting.Builder()
        .name("show-fps")
        .description("Show your framerate.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showUser = sgGeneral.add(new BoolSetting.Builder()
        .name("show-username")
        .description("Show your username.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> glow = sgGeneral.add(new BoolSetting.Builder()
        .name("glow")
        .description("Soft glow behind the bar.")
        .defaultValue(true)
        .build()
    );

    public EmberTopBarHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        // Colours come from the Ember theme so the bar recolours with the ClickGUI.
        Color accent = EmberPalette.accent();
        Color panel = EmberPalette.panel();
        Color bg = new Color(panel.r, panel.g, panel.b, 225);
        Color white = EmberPalette.textBright();
        Color gray = EmberPalette.textDim();
        Color sep = EmberPalette.divider();

        // The custom font rasterises a real typeface at scale*18px, so any scale is
        // smooth - no snapping, unlike Minecraft's bitmap font.
        double s = scale.get();
        double textScale = 0.95 * s;

        double textH = renderer.textHeight(true, textScale);
        double pad = 9 * s;
        double gap = 9 * s;
        double sepW = Math.max(1, s);
        double height = textH + 9 * s;
        double radius = 6 * s;

        List<String> parts = new ArrayList<>(5);
        List<Color> colors = new ArrayList<>(5);

        parts.add("Ember");
        colors.add(accent);

        if (showUser.get()) {
            parts.add(mc.getUser().getName());
            colors.add(white);
        }

        if (showPing.get()) {
            int ping = 0;
            if (mc.getConnection() != null && mc.player != null) {
                var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info != null) ping = info.getLatency();
            }
            parts.add(ping + "ms");
            colors.add(gray);
        }

        if (showFps.get()) {
            parts.add(mc.getFps() + " FPS");
            colors.add(white);
        }

        if (showServer.get()) {
            parts.add(mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "Singleplayer");
            colors.add(gray);
        }

        double[] widths = new double[parts.size()];
        double total = pad * 2;
        for (int i = 0; i < parts.size(); i++) {
            widths[i] = renderer.textWidth(parts.get(i), true, textScale);
            total += widths[i];
            if (i < parts.size() - 1) total += gap * 2 + sepW;
        }

        setSize(total, height);

        renderer.glow(x, y + 2 * s, total, height, 6 * s, new Color(0, 0, 0, 90));
        if (glow.get()) renderer.glow(x, y, total, height, 10 * s, new Color(accent.r, accent.g, accent.b, 120));

        renderer.roundedQuad(x, y, total, height, radius, bg);
        renderer.roundedQuad(x + 3 * s, y + 4 * s, 2 * s, height - 8 * s, 1 * s, accent);

        double ty = y + (height - textH) / 2;
        double cx = x + pad;

        for (int i = 0; i < parts.size(); i++) {
            renderer.text(parts.get(i), cx, ty, colors.get(i), true, textScale);
            cx += widths[i];

            if (i < parts.size() - 1) {
                cx += gap;
                renderer.quad(cx, y + 5 * s, sepW, height - 10 * s, sep);
                cx += sepW + gap;
            }
        }
    }
}
