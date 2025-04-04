/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GpuBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSystem.ShapeIndexBuffer.class)
public interface ShapeIndexBufferAccessor {
    @Accessor("buffer")
    GpuBuffer getBuffer();
}
