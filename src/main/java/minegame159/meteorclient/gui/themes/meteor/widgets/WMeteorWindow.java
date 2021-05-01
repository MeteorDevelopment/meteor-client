/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.themes.meteor.widgets;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.themes.meteor.MeteorWidget;
import minegame159.meteorclient.gui.widgets.containers.WWindow;

public class WMeteorWindow extends WWindow implements MeteorWidget {
    public WMeteorWindow(String title) {
        super(title);
    }

    @Override
    protected WHeader header() {
        return new WMeteorHeader();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animProgress > 0) {
            renderer.quadRounded(x, y + header.height / 2, width, height - header.height / 2, theme().backgroundColor.get(), theme.roundAmount(), false);
        }
    }

    private class WMeteorHeader extends WHeader {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.quadRounded(this, theme().accentColor.get(), theme.roundAmount());
        }
    }
}
