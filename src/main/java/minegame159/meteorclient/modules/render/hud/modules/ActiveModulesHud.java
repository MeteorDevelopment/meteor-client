/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.render.color.Color;

import java.util.ArrayList;
import java.util.List;

public class ActiveModulesHud extends HudModule {
    public enum Sort {
        Biggest,
        Smallest
    }

    public enum ColorMode {
        Flat,
        Random,
        Rainbow
    }

    private final List<Module> modules = new ArrayList<>();
//    private boolean update = true;

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

            if (hud.activeModulesSort.get() == Sort.Smallest) {
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

        rainbowHue1 += hud.activeModulesRainbowSpeed.get() * renderer.delta;
        if (rainbowHue1 > 1) rainbowHue1 -= 1;
        else if (rainbowHue1 < -1) rainbowHue1 += 1;

        rainbowHue2 = rainbowHue1;

        for (Module module : modules) {
            renderModule(renderer, module, x + box.alignX(getModuleWidth(renderer, module)), y);

            y += 2 + renderer.textHeight();
        }
    }

    private void renderModule(HudRenderer renderer, Module module, double x, double y) {
        Color color = hud.activeModulesFlatColor.get();

        ColorMode colorMode = hud.activeModulesColorMode.get();
        if (colorMode == ColorMode.Random) color = module.color;
        else if (colorMode == ColorMode.Rainbow) {
            rainbowHue2 += hud.activeModulesRainbowSpread.get();
            int c = java.awt.Color.HSBtoRGB((float) rainbowHue2, 1, 1);

            rainbow.r = Color.toRGBAR(c);
            rainbow.g = Color.toRGBAG(c);
            rainbow.b = Color.toRGBAB(c);

            color = rainbow;
        }
        
        renderer.text(module.title, x, y, color);

        if (hud.activeInfo.get()) {
            String info = module.getInfoString();
            if (info != null) renderer.text(info, x + renderer.textWidth(module.title) + renderer.textWidth(" "), y, hud.secondaryColor.get());
        }
    }

    private double getModuleWidth(HudRenderer renderer, Module module) {
        double width = renderer.textWidth(module.title);

        if (hud.activeInfo.get()) {
            String info = module.getInfoString();
            if (info != null) width += renderer.textWidth(" ") + renderer.textWidth(info);
        }

        return width;
    }
}
