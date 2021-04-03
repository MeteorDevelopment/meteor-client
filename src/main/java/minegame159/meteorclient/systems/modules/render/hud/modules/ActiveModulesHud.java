/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render.hud.modules;

import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.render.hud.HUD;
import minegame159.meteorclient.systems.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;

import java.util.ArrayList;
import java.util.List;

public class ActiveModulesHud extends HudElement {
    public enum Sort {
        Biggest,
        Smallest
    }

    public enum ColorMode {
        Flat,
        Random,
        Rainbow
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Sort> sort = sgGeneral.add(new EnumSetting.Builder<ActiveModulesHud.Sort>()
            .name("sort")
            .description("How to sort active modules.")
            .defaultValue(ActiveModulesHud.Sort.Biggest)
            .build()
    );

    private final Setting<Boolean> activeInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("additional-info")
            .description("Shows additional info from the module next to the name in the active modules list.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ActiveModulesHud.ColorMode> colorMode = sgGeneral.add(new EnumSetting.Builder<ActiveModulesHud.ColorMode>()
            .name("color-mode")
            .description("What color to use for active modules.")
            .defaultValue(ActiveModulesHud.ColorMode.Rainbow)
            .build()
    );

    private final Setting<SettingColor> flatColor = sgGeneral.add(new ColorSetting.Builder()
            .name("flat-color")
            .description("Color for flat color mode.")
            .defaultValue(new SettingColor(225, 25, 25))
            .build()
    );

    private final Setting<Double> rainbowSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("rainbow-speed")
            .description("Rainbow speed of rainbow color mode.")
            .defaultValue(0.025)
            .sliderMax(0.1)
            .decimalPlaces(4)
            .build()
    );

    private final Setting<Double> rainbowSpread = sgGeneral.add(new DoubleSetting.Builder()
            .name("rainbow-spread")
            .description("Rainbow spread of rainbow color mode.")
            .defaultValue(0.025)
            .sliderMax(0.05)
            .decimalPlaces(4)
            .build()
    );

    private final List<Module> modules = new ArrayList<>();

    private final Color rainbow = new Color(255, 255, 255);
    private double rainbowHue1, rainbowHue2;

    public ActiveModulesHud(HUD hud) {
        super(hud, "active-modules", "Displays your active modules.");
    }

    @Override
    public void update(HudRenderer renderer) {
        if (Modules.get() == null) {
            box.setSize(renderer.textWidth("Active Modules"), renderer.textHeight());
            return;
        }

        modules.clear();

        for (Module module : Modules.get().getActive()) {
            if (module.isVisible()) modules.add(module);
        }

        modules.sort((o1, o2) -> {
            double _1 = getModuleWidth(renderer, o1);
            double _2 = getModuleWidth(renderer, o2);

            if (sort.get() == Sort.Smallest) {
                double temp = _1;
                _1 = _2;
                _2 = temp;
            }

            int a = Double.compare(_1, _2);
            if (a == 0) return 0;
            return a < 0 ? 1 : -1;
        });

        double width = 0;
        double height = 0;

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);

            width = Math.max(width, getModuleWidth(renderer, module));
            height += renderer.textHeight();
            if (i > 0) height += 2;
        }

        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if (Modules.get() == null) {
            renderer.text("Active Modules", x, y, hud.color);
            return;
        }

        rainbowHue1 += rainbowSpeed.get() * renderer.delta;
        if (rainbowHue1 > 1) rainbowHue1 -= 1;
        else if (rainbowHue1 < -1) rainbowHue1 += 1;

        rainbowHue2 = rainbowHue1;

        for (Module module : modules) {
            renderModule(renderer, module, x + box.alignX(getModuleWidth(renderer, module)), y);

            y += 2 + renderer.textHeight();
        }
    }

    private void renderModule(HudRenderer renderer, Module module, double x, double y) {
        Color color = flatColor.get();

        ColorMode colorMode = this.colorMode.get();
        if (colorMode == ColorMode.Random) color = module.color;
        else if (colorMode == ColorMode.Rainbow) {
            rainbowHue2 += rainbowSpread.get();
            int c = java.awt.Color.HSBtoRGB((float) rainbowHue2, 1, 1);

            rainbow.r = Color.toRGBAR(c);
            rainbow.g = Color.toRGBAG(c);
            rainbow.b = Color.toRGBAB(c);

            color = rainbow;
        }
        
        renderer.text(module.title, x, y, color);

        if (activeInfo.get()) {
            String info = module.getInfoString();
            if (info != null) renderer.text(info, x + renderer.textWidth(module.title) + renderer.textWidth(" "), y, hud.secondaryColor.get());
        }
    }

    private double getModuleWidth(HudRenderer renderer, Module module) {
        double width = renderer.textWidth(module.title);

        if (activeInfo.get()) {
            String info = module.getInfoString();
            if (info != null) width += renderer.textWidth(" ") + renderer.textWidth(info);
        }

        return width;
    }
}
