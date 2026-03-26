/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderPassBackend;
import meteordevelopment.meteorclient.gui.renderer.ScissorState;
import meteordevelopment.meteorclient.mixininterface.IGpuDevice;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")
public abstract class GlBackendMixin implements IGpuDevice {
    @Override
    public void meteor$pushScissor(int x, int y, int width, int height) {
        ScissorState.push(x, y, width, height);
    }

    @Override
    public void meteor$popScissor() {
        ScissorState.pop();
    }

    @Deprecated
    @Override
    public void meteor$onCreateRenderPass(RenderPassBackend pass) {
        ScissorState.onCreateRenderPass(pass);
    }
}
