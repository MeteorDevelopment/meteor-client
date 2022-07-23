/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.renderer;

import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;

public abstract class GuiRenderOperation<T extends GuiRenderOperation<T>> {
    protected double x, y;
    protected Color color;

    public void set(double x, double y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public void run(Pool<T> pool) {
        onRun();
        pool.free((T) this);
    }

    protected abstract void onRun();
}
