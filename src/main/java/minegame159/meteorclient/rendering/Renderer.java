/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.rendering;

import minegame159.meteorclient.events.RenderEvent;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;

public class Renderer {
    public static final MeshBuilder TRIANGLES = new MeshBuilder();
    public static final MeshBuilder LINES = new MeshBuilder();

    private static boolean building;

    public static void begin(RenderEvent event) {
        if (!building) {
            TRIANGLES.begin(event, GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR);
            LINES.begin(event, GL11.GL_LINES, VertexFormats.POSITION_COLOR);

            building = true;
        }
    }

    public static void beginGui() {
        if (!building) {
            TRIANGLES.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);
            LINES.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);

            building = true;
        }
    }

    public static void end(boolean texture) {
        if (building) {
            TRIANGLES.end(texture);
            LINES.end(false);

            building = false;
        }
    }
    public static void end() {
        end(false);
    }

    public static boolean isBuilding() {
        return building;
    }
}
