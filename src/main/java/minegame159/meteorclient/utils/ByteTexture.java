package minegame159.meteorclient.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.resource.ResourceManager;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11C.*;

public class ByteTexture extends AbstractTexture {
    public ByteTexture(int width, int height, byte[] data) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> upload(width, height, data));
        } else {
            upload(width, height, data);
        }
    }

    private void upload(int width, int height, byte[] data) {
        TextureUtil.allocate(getGlId(), width, height);
        bindTexture();

        ByteBuffer buffer = BufferUtils.createByteBuffer(data.length).put(data);
        buffer.flip();

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, buffer);
    }

    @Override
    public void load(ResourceManager manager) throws IOException {}
}
