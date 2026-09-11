package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.EmberAnim;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.EmberPalette;

import java.util.*;

public class EmberModuleListHud extends HudElement {
    public static final HudElementInfo<EmberModuleListHud> INFO = new HudElementInfo<>(Hud.GROUP, "ember-module-list", "Your active modules, styled to match Ember.", EmberModuleListHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Size of the list.")
        .defaultValue(1.0)
        .min(0.5)
        .max(3.0)
        .sliderRange(0.5, 3.0)
        .build()
    );

    private final Setting<Sort> sort = sgGeneral.add(new EnumSetting.Builder<Sort>()
        .name("sort")
        .description("How to order active modules.")
        .defaultValue(Sort.Width)
        .build()
    );

    private final Setting<Alignment> alignment = sgGeneral.add(new EnumSetting.Builder<Alignment>()
        .name("alignment")
        .description("Which side rows line up on.")
        .defaultValue(Alignment.Auto)
        .build()
    );

    private final Setting<Boolean> showInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("module-info")
        .description("Show each module's extra info next to its name.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> glow = sgGeneral.add(new BoolSetting.Builder()
        .name("glow")
        .description("Soft glow behind each row.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Module>> hiddenModules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("hidden-modules")
        .description("Modules not to show in the list.")
        .build()
    );

    /** Modules on screen, including ones still animating out. */
    private final Map<Module, Float> anims = new LinkedHashMap<>();
    private final EmberAnim.Clock clock = new EmberAnim.Clock();

    public EmberModuleListHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        float dt = clock.tick();

        double s = scale.get();
        double textScale = 0.9 * s;
        double textH = renderer.textHeight(true, textScale);
        double rowH = textH + 8 * s;
        double gap = 2 * s;
        double pad = 8 * s;
        double bar = 2.5 * s;
        double radius = 5 * s;

        Color accent = EmberPalette.accent();
        Color panel = EmberPalette.panel();
        Color bright = EmberPalette.textBright();
        Color dim = EmberPalette.textDim();

        Set<Module> active = new HashSet<>();
        for (Module module : Modules.get().getActive()) {
            if (!hiddenModules.get().contains(module)) active.add(module);
        }
        for (Module module : active) anims.putIfAbsent(module, 0f);

        List<Module> shown = new ArrayList<>();
        Iterator<Map.Entry<Module, Float>> it = anims.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Module, Float> entry = it.next();
            float target = active.contains(entry.getKey()) ? 1f : 0f;
            float a = EmberAnim.approach(entry.getValue(), target, dt, 0.07);

            if (target == 0f && a <= 0.01f) {
                it.remove();
                continue;
            }

            entry.setValue(a);
            shown.add(entry.getKey());
        }

        if (shown.isEmpty()) {
            if (isInEditor()) {
                String label = "Module List";
                double w = renderer.textWidth(label, true, textScale) + pad * 2;
                setSize(w, rowH);
                renderer.roundedQuad(x, y, w, rowH, radius, new Color(panel.r, panel.g, panel.b, 200));
                renderer.text(label, x + pad, y + (rowH - textH) / 2, bright, true, textScale);
            } else {
                setSize(0, 0);
            }
            return;
        }

        Map<Module, Double> widths = new HashMap<>();
        for (Module module : shown) widths.put(module, rowWidth(renderer, module, textScale, pad, bar));

        if (sort.get() == Sort.Alphabetical) shown.sort(Comparator.comparing((Module m) -> m.title));
        else shown.sort(Comparator.comparingDouble((Module m) -> -widths.get(m)));

        double maxW = 0;
        double totalH = 0;
        for (Module module : shown) {
            maxW = Math.max(maxW, widths.get(module));
            totalH += (rowH + gap) * anims.get(module);
        }
        setSize(maxW, Math.max(rowH, totalH));

        boolean fromRight = alignment.get() == Alignment.Right
            || (alignment.get() == Alignment.Auto && box.xAnchor == XAnchor.Right);

        double cy = y;
        for (Module module : shown) {
            float a = anims.get(module);
            double rw = widths.get(module);

            double slide = (1 - a) * Math.min(rw, 60 * s) * (fromRight ? 1 : -1);
            double rx = x + alignX(rw, alignment.get()) + slide;

            if (glow.get()) renderer.glow(rx, cy, rw, rowH, 6 * s, new Color(accent.r, accent.g, accent.b, (int) (70 * a)));
            renderer.roundedQuad(rx, cy, rw, rowH, radius, new Color(panel.r, panel.g, panel.b, (int) (215 * a)));

            double barX = fromRight ? rx + rw - bar - 3 * s : rx + 3 * s;
            renderer.roundedQuad(barX, cy + 4 * s, bar, rowH - 8 * s, bar / 2, new Color(accent.r, accent.g, accent.b, (int) (255 * a)));

            double tx = fromRight ? rx + pad : rx + pad + bar + 2 * s;
            double ty = cy + (rowH - textH) / 2;
            renderer.text(module.title, tx, ty, new Color(bright.r, bright.g, bright.b, (int) (255 * a)), true, textScale);

            String info = showInfo.get() ? module.getInfoString() : null;
            if (info != null) {
                double titleW = renderer.textWidth(module.title, true, textScale);
                renderer.text(" " + info, tx + titleW, ty, new Color(dim.r, dim.g, dim.b, (int) (255 * a)), true, textScale);
            }

            cy += (rowH + gap) * a;
        }
    }

    private double rowWidth(HudRenderer renderer, Module module, double textScale, double pad, double bar) {
        double width = pad * 2 + bar + 2 * scale.get() + renderer.textWidth(module.title, true, textScale);

        if (showInfo.get()) {
            String info = module.getInfoString();
            if (info != null) width += renderer.textWidth(" " + info, true, textScale);
        }

        return width;
    }

    public enum Sort {
        Width,
        Alphabetical
    }
}
