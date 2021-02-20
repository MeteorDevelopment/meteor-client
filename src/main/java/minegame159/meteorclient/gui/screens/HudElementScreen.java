/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.events.render.Render2DEvent;
import minegame159.meteorclient.gui.widgets.WCheckbox;
import minegame159.meteorclient.gui.widgets.WHorizontalSeparator;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.render.hud.HUD;
import minegame159.meteorclient.modules.render.hud.modules.HudElement;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.util.math.MatrixStack;

public class HudElementScreen extends WindowScreen {
    private final HudElement element;

    public HudElementScreen(HudElement element) {
        super(element.title, true);
        this.element = element;

        initModules();
    }

    private void initModules() {
        // Description
        add(new WLabel(element.description));
        row();

        // Settings
        if (element.settings.sizeGroups() > 0) {
            add(element.settings.createTable(false)).fillX().expandX().getWidget();
            row();

            add(new WHorizontalSeparator());
            row();
        }

        // Bottom
        WTable bottomTable = add(new WTable()).fillX().expandX().getWidget();

        //   Active
        bottomTable.add(new WLabel("Active:"));
        WCheckbox active = bottomTable.add(new WCheckbox(element.active)).getWidget();
        active.action = () -> {
            if (element.active != active.checked) element.toggle();
        };
    }

    @Override
    protected void onRenderBefore(float delta) {
        Modules.get().get(HUD.class).onRender(Render2DEvent.get(0, 0, delta));
    }
}
