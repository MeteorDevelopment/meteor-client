/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.misc.CursorStyle;

public abstract class WPressable extends WWidget {
    public Runnable action;

    protected boolean pressed;

    @Override
    protected boolean onMouseClicked(boolean used, int button) {
        if (used) return false;

        if (mouseOver) pressed = true;
        return mouseOver;
    }

    @Override
    protected boolean onMouseReleased(boolean used, int button) {
        if (mouseOver && pressed) {
            onAction(button);
            if (action != null && runAction()) action.run();
        }

        pressed = false;
        return mouseOver && !used;
    }

    protected void onAction(int button) {}

    protected boolean runAction() {
        return true;
    }

    @Override
    public void render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (mouseOver) renderer.setCursorStyle(CursorStyle.Click);

        super.render(renderer, mouseX, mouseY, delta);
    }
}
