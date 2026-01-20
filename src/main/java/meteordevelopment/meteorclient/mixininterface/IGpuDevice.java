/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import org.jspecify.annotations.Nullable;

public interface IGpuDevice {
    /**
     * Currently there can only be a single scissor pushed at once.
     */
    void meteor$pushScissor(int x, int y, int width, int height);

    void meteor$popScissor();

    // FIXME this should be handled inside GlBackend createTexture() instead of a separate method, but if it works, it works.
    GpuTexture meteor$createAnimatedTexture(@Nullable String name, @GpuTexture.Usage int usage, TextureFormat textureFormat, int width, int height, int layers, int mipLevels);

    /**
     * This is an *INTERNAL* method, it shouldn't be called.
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    void meteor$onCreateRenderPass(RenderPass pass);
}
