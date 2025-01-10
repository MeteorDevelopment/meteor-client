/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import org.meteordev.juno.api.buffer.Buffer;
import org.meteordev.juno.api.commands.CommandList;
import org.meteordev.juno.utils.MeshBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO: Remove with Juno update
@Mixin(value = MeshBuilder.class, remap = false)
public abstract class MeshBuilderMixin {
    @Shadow
    private Buffer vbo;

    @Inject(method = "upload", at = @At(value = "INVOKE", target = "Lorg/meteordev/juno/api/buffer/Buffer;invalidate()V", ordinal = 1))
    private void onUpload(CommandList commands, CallbackInfo info) {
        if (vbo != null) {
            vbo.invalidate();
            vbo = null;
        }
    }
}
