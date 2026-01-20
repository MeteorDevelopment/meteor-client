/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import com.mojang.blaze3d.textures.GpuTexture;
import meteordevelopment.meteorclient.renderer.texture.AnimatedNativeImage;

public interface IGlCommandEncoder {
    void meteor_client$writeToAnimTexture(GpuTexture gpuTexture, AnimatedNativeImage nativeImage);
    void meteor_client$writeToAnimTexture(GpuTexture gpuTexture, AnimatedNativeImage nativeImage, int mipLevel, int layer, int dstOffsetX,
                                          int dstOffsetY, int dstOffsetZ, int width, int height, int srcOffsetX, int srcOffsetY, int srcOffsetZ);
}
