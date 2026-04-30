/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import net.minecraft.client.renderer.MultiBufferSource;

public interface IVertexConsumerProvider extends MultiBufferSource {
    void setOffset(int offsetX, int offsetY, int offsetZ);
}
