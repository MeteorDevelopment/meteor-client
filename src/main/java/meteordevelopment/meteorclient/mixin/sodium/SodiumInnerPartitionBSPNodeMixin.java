/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.InnerPartitionBSPNode", remap = false)
public class SodiumInnerPartitionBSPNodeMixin {
    // https://github.com/LlamaLad7/MixinExtras/wiki/Expressions#things-to-watch-out-for
    // 'Be particularly careful when using == 0 or != 0'
    @Definition(id = "splitPlaneEdgeDot", local = @Local(type = float.class, name = "splitPlaneEdgeDot"))
    @Expression("splitPlaneEdgeDot == 0.0")
    @Inject(method = "interpolateAttributes(FLorg/joml/Vector3fc;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;)V", at = @At("MIXINEXTRAS:EXPRESSION"), cancellable = true)
    private static void onInterpolateAttributes(float splitDistance, Vector3fc splitPlane,
        ChunkVertexEncoder.Vertex inside, ChunkVertexEncoder.Vertex outside, ChunkVertexEncoder.Vertex targetA,
        ChunkVertexEncoder.Vertex targetB, ChunkVertexEncoder.Vertex targetC, CallbackInfo ci,
        @Local(name = "splitPlaneEdgeDot") float splitPlaneEdgeDot) {
        if (splitPlaneEdgeDot == 0.0f) ci.cancel();
    }
}
