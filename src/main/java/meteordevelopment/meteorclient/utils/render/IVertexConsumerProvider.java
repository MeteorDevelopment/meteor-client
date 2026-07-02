/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;

public interface IVertexConsumerProvider {
    VertexConsumer getBuffer(RenderType layer);

    void setOffset(int offsetX, int offsetY, int offsetZ);
}
