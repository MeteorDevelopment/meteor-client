/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.ActiveModulesChangedEvent;
import minegame159.meteorclient.events.ModuleVisibilityChangedEvent;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.HudRenderer;

import java.util.ArrayList;
import java.util.List;

public class ActiveModulesHud extends HudModule {
    public enum Sort {
        ByBiggest,
        BySmallest
    }

    private final List<ToggleModule> modules = new ArrayList<>();
    private boolean update = true;

    public ActiveModulesHud(HUD hud) {
        super(hud, "active-modules", "Displays your active modules.");

        MeteorClient.EVENT_BUS.subscribe(new Listener<ActiveModulesChangedEvent>(event -> update = true));
        MeteorClient.EVENT_BUS.subscribe(new Listener<ModuleVisibilityChangedEvent>(event -> update = true));
    }

    public void recalculate() {
        update = true;
    }

    @Override
    public void update(HudRenderer renderer) {
        if (ModuleManager.INSTANCE == null) {
            box.setSize(renderer.textWidth("Active Modules"), renderer.textHeight());
            return;
        }

        if (!update) return;
        update = false;
        modules.clear();

        for (ToggleModule module : ModuleManager.INSTANCE.getActive()) {
            if (module.isVisible()) modules.add(module);
        }

        modules.sort((o1, o2) -> {
            double _1 = getModuleWidth(renderer, o1);
            double _2 = getModuleWidth(renderer, o2);

            if (hud.activeModulesSort() == Sort.BySmallest) {
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
            ToggleModule module = modules.get(i);

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

        if (ModuleManager.INSTANCE == null) {
            renderer.text("Active Modules", x, y, hud.color);
            return;
        }

        for (ToggleModule module : modules) {
            renderModule(renderer, module, x + box.alignX(getModuleWidth(renderer, module)), y);

            y += 2 + renderer.textHeight();
        }
    }

    private void renderModule(HudRenderer renderer, ToggleModule module, double x, double y) {
        renderer.text(module.title, x, y, module.color);

        String info = module.getInfoString();
        if (info != null) {
            renderer.text(info, x + renderer.textWidth(module.title) + renderer.textWidth(" "), y, hud.secondaryColor());
        }
    }

    private double getModuleWidth(HudRenderer renderer, ToggleModule module) {
        String info = module.getInfoString();
        double width = renderer.textWidth(module.title);
        if (info != null) width += renderer.textWidth(" ") + renderer.textWidth(info);
        return width;
    }
}
