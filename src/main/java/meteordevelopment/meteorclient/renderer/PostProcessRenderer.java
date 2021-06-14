/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import net.minecraft.client.util.math.MatrixStack;

public class PostProcessRenderer {
    private static Mesh mesh;
    private static final MatrixStack matrices = new MatrixStack();

    public static void init() {
        mesh = new Mesh(DrawMode.Triangles, Mesh.Attrib.Vec2);
        mesh.begin();

        mesh.quad(
            mesh.vec2(-1, -1).next(),
            mesh.vec2(-1, 1).next(),
            mesh.vec2(1, 1).next(),
            mesh.vec2(1, -1).next()
        );

        mesh.end();
    }

    public static void render() {
        mesh.render(matrices);
    }
}
