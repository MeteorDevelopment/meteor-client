/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudRenderer;

public class ModuleInfoHud extends HudModule {
    public ModuleInfoHud(HUD hud) {
        super(hud, "module-info", "Displays if selected modules are enabled or disabled.");
    }

    @Override
    public void update(HudRenderer renderer) {
        if (Modules.get() == null || hud.moduleInfoModules.get().isEmpty()) {
            box.setSize(renderer.textWidth("Module Info"), renderer.textHeight());
            return;
        }

        double width = 0;
        double height = 0;

        int i = 0;
        for (Module module : hud.moduleInfoModules.get()) {
            width = Math.max(width, getModuleWidth(renderer, module));
            height += renderer.textHeight();
            if (i > 0) height += 2;

            i++;
        }

        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        if (Modules.get() == null || hud.moduleInfoModules.get().isEmpty()) {
            renderer.text("Module Info", x, y, hud.primaryColor.get());
            return;
        }

        for (Module module : hud.moduleInfoModules.get()) {
            renderModule(renderer, module, x + box.alignX(getModuleWidth(renderer, module)), y);

            y += 2 + renderer.textHeight();
        }
    }

    private void renderModule(HudRenderer renderer, Module module, double x, double y) {
        renderer.text(module.title, x, y, hud.primaryColor.get());

        String info = getModuleInfo(module);
        renderer.text(info,x + renderer.textWidth(module.title) + renderer.textWidth(" "), y, module.isActive() ? hud.moduleInfoOnColor.get() : hud.moduleInfoOffColor.get());
    }

    private double getModuleWidth(HudRenderer renderer, Module module) {
        double width = renderer.textWidth(module.title);
        if (hud.moduleInfo.get()) width += renderer.textWidth(" ") + renderer.textWidth(getModuleInfo(module));
        return width;
    }

    private String getModuleInfo(Module module) {
        if (module.getInfoString() != null && module.isActive() && hud.moduleInfo.get()) return module.getInfoString();
        else if (module.isActive()) return "ON";
        else return "OFF";
    }
}
