package minegame159.meteorclient.rendering;

import org.lwjgl.opengl.GL11;

public enum DrawMode {
    Triangles,
    Lines;

    public int toOpenGl() {
        if (this == Triangles) return GL11.GL_TRIANGLES;
        return GL11.GL_LINES;
    }
}
