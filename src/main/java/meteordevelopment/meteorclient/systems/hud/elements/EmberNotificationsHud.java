package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.EmberAnim;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.EmberPalette;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EmberNotificationsHud extends HudElement {
    public static final HudElementInfo<EmberNotificationsHud> INFO = new HudElementInfo<>(Hud.GROUP, "ember-notifications", "Themed pop-ups for module toggles and Ember alerts.", EmberNotificationsHud::new);

    public static final Color GOOD = new Color(93, 217, 127, 255);
    public static final Color BAD = new Color(222, 86, 86, 255);

    private static final ConcurrentLinkedQueue<Toast> INCOMING = new ConcurrentLinkedQueue<>();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Size of the notifications.")
        .defaultValue(1.0)
        .min(0.5)
        .max(3.0)
        .sliderRange(0.5, 3.0)
        .build()
    );

    private final Setting<Double> duration = sgGeneral.add(new DoubleSetting.Builder()
        .name("duration")
        .description("Seconds each notification stays on screen.")
        .defaultValue(2.5)
        .min(0.5)
        .sliderRange(0.5, 10)
        .build()
    );

    private final Setting<Integer> maxShown = sgGeneral.add(new IntSetting.Builder()
        .name("max-shown")
        .description("Most notifications visible at once.")
        .defaultValue(5)
        .min(1)
        .sliderRange(1, 10)
        .build()
    );

    private final Setting<Boolean> moduleToggles = sgGeneral.add(new BoolSetting.Builder()
        .name("module-toggles")
        .description("Notify when a module is turned on or off.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> glow = sgGeneral.add(new BoolSetting.Builder()
        .name("glow")
        .description("Soft glow behind each notification.")
        .defaultValue(true)
        .build()
    );

    private final List<Toast> toasts = new ArrayList<>();
    private final EmberAnim.Clock clock = new EmberAnim.Clock();
    private Set<Module> lastActive;

    public EmberNotificationsHud() {
        super(INFO);
    }

    /** Shows a notification. Safe to call from any thread. */
    public static void push(String title, String subtitle, Color color) {
        INCOMING.add(new Toast(title, subtitle, color));

        // Nothing drains the queue while the widget is off, so don't let alerts pile up for later.
        while (INCOMING.size() > 20) INCOMING.poll();
    }

    private static final class Toast {
        final String title;
        final String subtitle;
        final Color color;
        double age;
        float anim;

        Toast(String title, String subtitle, Color color) {
            this.title = title;
            this.subtitle = subtitle;
            this.color = color;
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        float dt = clock.tick();
        trackModuleToggles();

        Toast incoming;
        while ((incoming = INCOMING.poll()) != null) toasts.add(incoming);

        // Too many at once: expire the oldest so they animate out instead of vanishing.
        int alive = 0;
        for (Toast toast : toasts) if (toast.age < duration.get()) alive++;
        for (Toast toast : toasts) {
            if (alive <= maxShown.get()) break;
            if (toast.age < duration.get()) {
                toast.age = duration.get();
                alive--;
            }
        }

        double s = scale.get();
        double titleScale = 0.9 * s;
        double subScale = 0.72 * s;
        double titleH = renderer.textHeight(true, titleScale);
        double subH = renderer.textHeight(true, subScale);

        double w = 200 * s;
        double pad = 9 * s;
        double rowH = pad * 2 + titleH + subH + 2 * s;
        double gap = 6 * s;
        double boxH = maxShown.get() * (rowH + gap);

        setSize(w, boxH);

        for (Toast toast : toasts) {
            toast.age += dt;
            float target = toast.age < duration.get() ? 1f : 0f;
            toast.anim = EmberAnim.approach(toast.anim, target, dt, 0.08);
        }
        toasts.removeIf(toast -> toast.age >= duration.get() && toast.anim <= 0.01f);

        Color accent = EmberPalette.accent();
        Color panel = EmberPalette.panel();

        if (toasts.isEmpty()) {
            if (isInEditor()) drawToast(renderer, "Notifications", "Module toggles and alerts", accent, 1f, x, y + boxH - rowH,
                w, rowH, pad, titleScale, subScale, titleH, s, panel, 1);
            return;
        }

        // Newest at the bottom, older ones stacked above it.
        double cy = y + boxH;
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast toast = toasts.get(i);
            cy -= (rowH + gap) * toast.anim;

            double slide = (1 - toast.anim) * 40 * s;
            double progress = Math.max(0, 1 - toast.age / duration.get());
            drawToast(renderer, toast.title, toast.subtitle, toast.color, toast.anim, x + slide, cy,
                w, rowH, pad, titleScale, subScale, titleH, s, panel, progress);
        }
    }

    private void drawToast(HudRenderer renderer, String title, String subtitle, Color color, float a,
                           double tx, double ty, double w, double h, double pad,
                           double titleScale, double subScale, double titleH, double s, Color panel, double progress) {
        if (a <= 0.01f) return;

        Color bright = EmberPalette.textBright();
        Color dim = EmberPalette.textDim();
        double radius = 7 * s;

        if (glow.get()) renderer.glow(tx, ty, w, h, 8 * s, withAlpha(color, (int) (80 * a)));
        renderer.roundedQuad(tx, ty, w, h, radius, new Color(panel.r, panel.g, panel.b, (int) (225 * a)));
        renderer.roundedQuad(tx + 4 * s, ty + 6 * s, 3 * s, h - 12 * s, 1.5 * s, withAlpha(color, (int) (255 * a)));

        double textX = tx + pad + 5 * s;
        renderer.text(title, textX, ty + pad, withAlpha(bright, (int) (255 * a)), true, titleScale);
        renderer.text(subtitle, textX, ty + pad + titleH + 2 * s, withAlpha(dim, (int) (255 * a)), true, subScale);

        // Time remaining.
        double barW = (w - 16 * s) * progress;
        if (barW > 1) renderer.roundedQuad(tx + 8 * s, ty + h - 4 * s, barW, 1.5 * s, 0.75 * s, withAlpha(color, (int) (140 * a)));
    }

    private void trackModuleToggles() {
        Set<Module> now = new HashSet<>(Modules.get().getActive());

        if (lastActive != null && moduleToggles.get()) {
            for (Module module : now) if (!lastActive.contains(module)) push(module.title, "Enabled", GOOD);
            for (Module module : lastActive) if (!now.contains(module)) push(module.title, "Disabled", BAD);
        }

        lastActive = now;
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.r, c.g, c.b, Math.max(0, Math.min(255, alpha)));
    }
}
