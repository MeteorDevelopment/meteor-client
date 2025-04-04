/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.renderer;

import org.lwjgl.opengl.GL32C;

public enum DrawMode {
    Lines(2),
    Triangles(3);

    public final int indicesCount;

    DrawMode(int indicesCount) {
        this.indicesCount = indicesCount;
    }

    public int getGL() {
        return switch (this) {
            case Lines ->     GL32C.GL_LINES;
            case Triangles -> GL32C.GL_TRIANGLES;
        };
    }
}
