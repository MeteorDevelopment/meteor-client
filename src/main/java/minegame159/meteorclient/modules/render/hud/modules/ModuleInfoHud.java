/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.ActiveModulesChangedEvent;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudRenderer;

public class ModuleInfoHud extends HudModule {
    private boolean update = true;

    public ModuleInfoHud(HUD hud) {
        super(hud, "module-info", "Displays if selected modules are active.");

        MeteorClient.EVENT_BUS.subscribe(new Listener<ActiveModulesChangedEvent>(event -> update = true));
    }

    public void recalculate() {
        update = true;
    }

    @Override
    public void update(HudRenderer renderer) {
        if (ModuleManager.INSTANCE == null || hud.moduleInfoModules().isEmpty()) {
            box.setSize(renderer.textWidth("Module Info"), renderer.textHeight());
            return;
        }

        if (!update) return;
        update = false;

        double width = 0;
        double height = 0;

        int i = 0;
        for (ToggleModule module : hud.moduleInfoModules()) {
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

        if (ModuleManager.INSTANCE == null || hud.moduleInfoModules().isEmpty()) {
            renderer.text("Module Info", x, y, hud.primaryColor());
            return;
        }

        for (ToggleModule module : hud.moduleInfoModules()) {
            renderModule(renderer, module, x + box.alignX(getModuleWidth(renderer, module)), y);

            y += 2 + renderer.textHeight();
        }
    }

    private void renderModule(HudRenderer renderer, ToggleModule module, double x, double y) {
        renderer.text(module.title, x, y, hud.primaryColor());
        renderer.text(module.isActive() ? " ON" : " OFF", x + renderer.textWidth(module.title), y, module.isActive() ? hud.moduleInfoOnColor() : hud.moduleInfoOffColor());
    }

    private double getModuleWidth(HudRenderer renderer, ToggleModule module) {
        double w = renderer.textWidth(module.title);

        if (module.isActive()) w += renderer.textWidth(" ON");
        else w += renderer.textWidth(" OFF");

        return w;
    }
}
