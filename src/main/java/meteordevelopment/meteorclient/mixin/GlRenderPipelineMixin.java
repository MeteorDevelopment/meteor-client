/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.renderpearl.backend.opengl.GlRenderPipeline;
import meteordevelopment.meteorclient.mixininterface.IRenderPipeline;
import org.lwjgl.opengl.GL11C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlRenderPipeline.class)
public abstract class GlRenderPipelineMixin implements IRenderPipeline {
    @Unique
    private boolean lineSmooth;

    @Override
    public void meteor$setLineSmooth(boolean lineSmooth) {
        this.lineSmooth = lineSmooth;
    }

    @Override
    public boolean meteor$getLineSmooth() {
        return lineSmooth;
    }

    @Inject(method = "bind", at = @At("RETURN"))
    private void bind$lineSmooth(CallbackInfo ci) {
        if (lineSmooth) {
            GL11C.glEnable(GL11C.GL_LINE_SMOOTH);
            GL11C.glLineWidth(1);
        } else {
            GL11C.glDisable(GL11C.GL_LINE_SMOOTH);
        }
    }
}
