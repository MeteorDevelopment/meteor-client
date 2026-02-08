/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import meteordevelopment.meteorclient.mixininterface.IGpuDevice;
import net.minecraft.client.gl.GlBackend;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.util.TextureAllocationException;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.nio.ByteBuffer;

@Mixin(GlBackend.class)
public abstract class GlBackendMixin implements IGpuDevice {
    @Unique
    private int x, y, width, height;

    @Unique
    private boolean set;

    @Override
    public void meteor$pushScissor(int x, int y, int width, int height) {
        if (set)
            throw new IllegalStateException("Currently there can only be one global scissor pushed");

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        set = true;
    }

    @Override
    public void meteor$popScissor() {
        if (!set)
            throw new IllegalStateException("No scissor pushed");

        set = false;
    }

    @Deprecated
    @Override
    public void meteor$onCreateRenderPass(RenderPass pass) {
        if (set) {
            pass.enableScissor(x, y, width, height);
        }
    }

    @Override
    public GpuTexture meteor$createAnimatedTexture(@Nullable String name, @GpuTexture.Usage int usage, TextureFormat textureFormat, int width, int height, int layers, int mipLevels) {
        if (mipLevels < 1) {
            throw new IllegalArgumentException("mipLevels must be at least 1");
        } else if (layers < 1) {
            throw new IllegalArgumentException("depthOrLayers must be at least 1");
        } else {
            boolean block = (usage & 16) != 0;

            if (block) {
                throw new UnsupportedOperationException("Cubemap compatible textures aren't implemented yet");
            }
            GlStateManager.clearGlErrors();
            int glId = GlStateManager._genTexture();

            if (name == null) {
                name = String.valueOf(glId);
            }
            int target = GL30.GL_TEXTURE_2D_ARRAY;
            GL11.glBindTexture(target, glId);

            GlStateManager._texParameter(target, GL12.GL_TEXTURE_MAX_LEVEL, mipLevels - 1);
            GlStateManager._texParameter(target, GL12.GL_TEXTURE_MIN_LOD, 0);
            GlStateManager._texParameter(target, GL12.GL_TEXTURE_MAX_LOD, mipLevels - 1);
            if (textureFormat.hasDepthAspect()) {
                GlStateManager._texParameter(target, GlConst.GL_TEXTURE_COMPARE_MODE, 0);
            }

            for (int mipLevel = 0; mipLevel < mipLevels; mipLevel++) {
                // FIXME should have a method in GlStateManager for this. (Another duck?)
                GL12.glTexImage3D(target, mipLevel, GlConst.toGlInternalId(textureFormat), width >> mipLevel, height >> mipLevel,
                    layers, 0, GlConst.toGlExternalId(textureFormat), GlConst.toGlType(textureFormat), (ByteBuffer) null);
            }

            int error = GlStateManager._getError();
            if (error == GlConst.GL_OUT_OF_MEMORY) {
                throw new TextureAllocationException("Could not allocate texture of " + width + "x" + height + " for " + name);
            } else if (error != 0) {
                throw new IllegalStateException("OpenGL error " + error);
            } else {
                return new GlTexture(usage, name, textureFormat, width, height, layers, mipLevels, glId);
            }
        }
    }
}
