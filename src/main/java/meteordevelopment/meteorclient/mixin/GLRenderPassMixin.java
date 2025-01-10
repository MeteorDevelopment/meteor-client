/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import org.lwjgl.opengl.GL33C;
import org.meteordev.juno.opengl.commands.GLRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO: Remove with Juno update
@Mixin(value = GLRenderPass.class, remap = false)
public abstract class GLRenderPassMixin {
    @Inject(method = "lambda$draw$3", at = @At("TAIL"))
    private void onDrawCommand(CallbackInfo info) {
        GL33C.glBindVertexArray(0);
    }
}
