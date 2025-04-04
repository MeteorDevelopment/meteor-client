/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.renderer;

import motordevelopment.motorclient.utils.PreInit;
import net.minecraft.client.util.math.MatrixStack;

public class PostProcessRenderer {
    private static Mesh mesh;
    private static final MatrixStack matrices = new MatrixStack();

    private PostProcessRenderer() {
    }

    @PreInit
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

    public static void beginRender() {
        mesh.beginRender(matrices);
    }

    public static void render() {
        mesh.render(matrices);
    }

    public static void endRender() {
        mesh.endRender();
    }
}
