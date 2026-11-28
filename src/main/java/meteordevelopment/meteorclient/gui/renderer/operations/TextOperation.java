/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.renderer.operations;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderOperation;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;

public class TextOperation extends GuiRenderOperation<TextOperation> {
    private String text;
    private TextRenderer renderer;

    public double scale = 1;

    public TextOperation set(String text, TextRenderer renderer, double scale) {
        this.text = text;
        this.renderer = renderer;
        this.scale = scale;

        return this;
    }

    @Override
    protected void onRun() {
        renderer.render(text, x, y, color);
    }
}
