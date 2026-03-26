/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.renderer;

import com.mojang.blaze3d.systems.RenderPassBackend;

public final class ScissorState {
    private static int x;
    private static int y;
    private static int width;
    private static int height;
    private static boolean set;

    private ScissorState() {}

    public static void push(int x, int y, int width, int height) {
        if (set) throw new IllegalStateException("Currently there can only be one global scissor pushed");

        ScissorState.x = x;
        ScissorState.y = y;
        ScissorState.width = width;
        ScissorState.height = height;
        set = true;
    }

    public static void pop() {
        if (!set) throw new IllegalStateException("No scissor pushed");
        set = false;
    }

    public static void onCreateRenderPass(RenderPassBackend pass) {
        if (set) pass.enableScissor(x, y, width, height);
    }
}
