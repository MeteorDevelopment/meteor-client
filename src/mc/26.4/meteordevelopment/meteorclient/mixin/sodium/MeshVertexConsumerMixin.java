/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import meteordevelopment.meteorclient.utils.render.MeshBuilderVertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 26.4: Sodium has no build for this Minecraft version yet, and its
 * {@code VertexBufferWriter#push} is compiled against the VertexFormat package that 26.3 used
 * ({@code com.mojang.renderpearl.api.vertex}), which no longer exists. The interface therefore
 * cannot be implemented here; the mixin is kept so that the shared Sodium mixin config stays
 * valid. It is never applied, because the mixin plugin only applies it when Sodium is loaded.
 */
@Mixin(value = MeshBuilderVertexConsumerProvider.MeshBuilderVertexConsumer.class, remap = false)
public abstract class MeshVertexConsumerMixin {
}
