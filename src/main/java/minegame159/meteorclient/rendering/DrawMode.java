package minegame159.meteorclient.rendering;

import org.lwjgl.opengl.GL11;

public enum DrawMode {
    Triangles,
    Lines,
    Quads;

    public int toOpenGl() {
        if (this == Triangles) return GL11.GL_TRIANGLES;
        else if (this == Quads) return GL11.GL_QUADS;
        return GL11.GL_LINES;
    }
}
