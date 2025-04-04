/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixininterface;

import net.minecraft.client.gl.Framebuffer;

public interface IWorldRenderer {
    void motor$pushEntityOutlineFramebuffer(Framebuffer framebuffer);

    void motor$popEntityOutlineFramebuffer();
}
