/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import org.lwjgl.opengl.GL33C;
import org.meteordev.juno.opengl.GLState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO: Remove with Juno update
@Mixin(value = GLState.class, remap = false)
public abstract class GLStateMixin {
    @Shadow
    public int srcColor;

    @Shadow
    public int dstColor;

    @Shadow
    public int srcAlpha;

    @Shadow
    public int dstAlpha;

    @Shadow
    public int depthFunc;

    @Shadow
    public int cullFace;

    @Inject(method = "syncWith", at = @At("TAIL"))
    private void onSyncWith(GLState other, CallbackInfo info) {
        if (this.srcColor != other.srcColor || this.dstColor != other.dstColor || this.srcAlpha != other.srcAlpha || this.dstAlpha != other.dstAlpha) {
            GL33C.glBlendFuncSeparate(other.srcColor, other.dstColor, other.srcAlpha, other.dstAlpha);
            this.srcColor = other.srcColor;
            this.dstColor = other.dstColor;
            this.srcAlpha = other.srcAlpha;
            this.dstAlpha = other.dstAlpha;
        }

        if (this.depthFunc != other.depthFunc) {
            GL33C.glDepthFunc(other.depthFunc);
            this.depthFunc = other.depthFunc;
        }

        if (this.cullFace != other.cullFace) {
            GL33C.glCullFace(other.cullFace);
            this.cullFace = other.cullFace;
        }
    }
}
