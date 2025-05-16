/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.utils.PreInit;

public class FullScreenRenderer {
    public static MeshBuilder mesh;

    private FullScreenRenderer() {}

    @PreInit
    public static void init() {
        mesh = new MeshBuilder(MeteorVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES, 4, 6);

        mesh.begin();
        mesh.ensureQuadCapacity();

        mesh.quad(
            mesh.vec2(-1, -1).next(),
            mesh.vec2(-1, 1).next(),
            mesh.vec2(1, 1).next(),
            mesh.vec2(1, -1).next()
        );

        mesh.end();
    }
}
