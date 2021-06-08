package minegame159.meteorclient.renderer;

import org.lwjgl.opengl.GL32C;

public enum DrawMode {
    Lines,
    Triangles;

    public int getGL() {
        return switch (this) {
            case Lines ->     GL32C.GL_LINES;
            case Triangles -> GL32C.GL_TRIANGLES;
        };
    }
}
