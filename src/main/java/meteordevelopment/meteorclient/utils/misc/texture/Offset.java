/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.texture;

public record Offset(int x, int y) {
    /**
     * Get the offset in the animated atlas for a certain frame in an animated texture. The atlas adds frames row first
     * before adding new columns.
     * @return Offset.
     */
    public static Offset getFrameOffset(int i, int framesPerColumn, int width, int height) {
        int col = i / framesPerColumn;
        int row = i % framesPerColumn;
        int x = col * width;
        int y = row * height;
        return new Offset(x,y);
    }
}
