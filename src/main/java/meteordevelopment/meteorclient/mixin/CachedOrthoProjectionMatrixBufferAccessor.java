/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CachedOrthoProjectionMatrixBuffer.class)
public interface CachedOrthoProjectionMatrixBufferAccessor {
    @Invoker("createProjectionMatrix")
    Matrix4f meteor$callCreateProjectionMatrix(float width, float height);
}
