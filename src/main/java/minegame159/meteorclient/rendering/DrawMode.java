/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.rendering;

import net.minecraft.client.render.VertexFormat;
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

    public VertexFormat.DrawMode getDrawMode() {
        return switch (this) {
            case Triangles -> VertexFormat.DrawMode.TRIANGLES;
            case Lines     -> VertexFormat.DrawMode.DEBUG_LINES;
            case Quads     -> VertexFormat.DrawMode.QUADS;
        };
    }
}
