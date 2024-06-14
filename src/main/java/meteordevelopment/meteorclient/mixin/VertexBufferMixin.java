/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.renderer.GL;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.util.BufferAllocator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
public abstract class VertexBufferMixin {
    @Shadow private int indexBufferId;

    @Inject(method = "uploadIndexBuffer", at = @At("RETURN"))
    private void onConfigureIndexBuffer(BufferAllocator.CloseableBuffer closeableBuffer, CallbackInfo info) {
        if (info == null) GL.CURRENT_IBO = this.indexBufferId;
        else GL.CURRENT_IBO = ((ShapeIndexBufferAccessor) info).getId();
    }
}
