/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.render.BufferRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferRenderer.class)
public interface BufferRendererAccessor {
    @Accessor("currentVertexArray")
    static void setCurrentVertexArray(int vao) {}

    @Accessor("currentVertexBuffer")
    static void setCurrentVertexBuffer(int vbo) {}

    @Accessor("currentElementBuffer")
    static void setCurrentElementBuffer(int ibo) {}
}
