/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.rendering;

import minegame159.meteorclient.events.render.Render3DEvent;
import net.minecraft.client.render.VertexFormats;

@Deprecated
public class Renderer {
    public static final MeshBuilder NORMAL = new MeshBuilder();
    public static final MeshBuilder LINES = new MeshBuilder();

    private static boolean building;

    public static void begin(Render3DEvent event) {
        if (!building) {
            NORMAL.begin(event, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
            LINES.begin(event, DrawMode.Lines, VertexFormats.POSITION_COLOR);

            building = true;
        }
    }

    public static void end() {
        if (building) {
            NORMAL.end();
            LINES.end();

            building = false;
        }
    }
}
