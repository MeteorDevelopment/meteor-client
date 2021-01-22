/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud;

import minegame159.meteorclient.rendering.text.TextRenderer;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.render.color.Color;

public class HudRenderer {
    public double delta;

    private boolean scaleOnly;

    public void begin(double scale, double tickDelta, boolean scaleOnly) {
        if (!scaleOnly) Utils.unscaledProjection();
        TextRenderer.get().begin(scale, scaleOnly, false);

        this.delta = tickDelta;
        this.scaleOnly = scaleOnly;
    }

    public void end() {
        TextRenderer.get().end();
        if (!scaleOnly) Utils.scaledProjection();
    }

    public void text(String text, double x, double y, Color color) {
        TextRenderer.get().render(text, x, y, color, true);
    }

    public double textWidth(String text) {
        return TextRenderer.get().getWidth(text);
    }

    public double textHeight() {
        return TextRenderer.get().getHeight();
    }
}
