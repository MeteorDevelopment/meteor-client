/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.resource.ResourceManager;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11C.*;

public class ByteTexture extends AbstractTexture {
    public ByteTexture(int width, int height, byte[] data, boolean text) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> upload(width, height, data, text));
        } else {
            upload(width, height, data, text);
        }
    }

    public ByteTexture(int width, int height, ByteBuffer buffer, boolean text) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> upload(width, height, buffer, text));
        } else {
            upload(width, height, buffer, text);
        }
    }

    private void upload(int width, int height, byte[] data, boolean text) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(data.length).put(data);
        ((Buffer) buffer).flip();

        upload(width, height, buffer, text);
    }

    private void upload(int width, int height, ByteBuffer buffer, boolean text) {
        TextureUtil.allocate(getGlId(), width, height);
        bindTexture();

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, text ? GL_LINEAR : GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, text ? GL_LINEAR : GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, text ? GL_ALPHA : GL_RGB, width, height, 0, text ? GL_ALPHA : GL_RGB, GL_UNSIGNED_BYTE, buffer);
    }

    @Override
    public void load(ResourceManager manager) throws IOException {}
}
