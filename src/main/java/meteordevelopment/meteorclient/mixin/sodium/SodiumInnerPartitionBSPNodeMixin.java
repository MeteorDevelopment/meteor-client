/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.InnerPartitionBSPNode", remap = false)
public class SodiumInnerPartitionBSPNodeMixin {
    private static final float VERTEX_EPSILON = 0.00001f;

    @Inject(method = "interpolateAttributes(FLorg/joml/Vector3fc;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;)V", at = @At("HEAD"), cancellable = true)
    private static void onInterpolateAttributes(float splitDistance, Vector3fc splitPlane,
            ChunkVertexEncoder.Vertex inside, ChunkVertexEncoder.Vertex outside, ChunkVertexEncoder.Vertex targetA,
            ChunkVertexEncoder.Vertex targetB, ChunkVertexEncoder.Vertex targetC, CallbackInfo ci) {
        float insideToOutsideX = outside.x - inside.x;
        float insideToOutsideY = outside.y - inside.y;
        float insideToOutsideZ = outside.z - inside.z;

        if (Math.abs(insideToOutsideX) < VERTEX_EPSILON && Math.abs(insideToOutsideY) < VERTEX_EPSILON && Math.abs(insideToOutsideZ) < VERTEX_EPSILON) {
            return;
        }

        float splitPlaneEdgeDot = splitPlane.dot(insideToOutsideX, insideToOutsideY, insideToOutsideZ);
        if (splitPlaneEdgeDot == 0.0F) {
            ci.cancel();
        }
    }
}
