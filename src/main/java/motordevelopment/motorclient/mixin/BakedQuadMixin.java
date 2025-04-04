/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.mixininterface.IBakedQuad;
import net.minecraft.client.render.model.BakedQuad;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BakedQuad.class)
public abstract class BakedQuadMixin implements IBakedQuad {
    @Shadow @Final protected int[] vertexData;

    @Override
    public float motor$getX(int vertexI) {
        return Float.intBitsToFloat(vertexData[vertexI * 8]);
    }

    @Override
    public float motor$getY(int vertexI) {
        return Float.intBitsToFloat(vertexData[vertexI * 8 + 1]);
    }

    @Override
    public float motor$getZ(int vertexI) {
        return Float.intBitsToFloat(vertexData[vertexI * 8 + 2]);
    }
}
