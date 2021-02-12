/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.renderer;

public class Region {
    public static final Region FULL = new Region(0, 0, 1, 1);
    public static final Region CIRCLE = new Region(1, 0, 64, 64);
    public static final Region EDIT = new Region(65, 0, 64, 64);
    public static final Region RESET = new Region(129, 0, 64, 64);

    private static final int TEX_WIDTH = 193;
    private static final int TEX_HEIGHT = 64;

    public final float x, y;
    public final float width, height;

    public Region(int x, int y, int width, int height) {
        this.x = (float) x / TEX_WIDTH;
        this.y = (float) y / TEX_HEIGHT;
        this.width = (float) width / TEX_WIDTH;
        this.height = (float) height / TEX_HEIGHT;
    }
}
