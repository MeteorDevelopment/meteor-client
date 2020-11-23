/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.renderer.GuiRenderer;

public abstract class WPressable extends WWidget {
    public Runnable action;

    protected boolean pressed;

    private boolean runAction;

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
        super.render(renderer, mouseX, mouseY, delta);
    }
}
