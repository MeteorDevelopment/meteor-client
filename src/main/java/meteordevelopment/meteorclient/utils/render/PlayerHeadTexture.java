package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.network.Http;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerHeadTexture extends Texture {
    public PlayerHeadTexture(String url) {
        BufferedImage skin;
        try {
            skin = ImageIO.read(Http.get(url).sendInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        byte[] head = new byte[8 * 8 * 3];
        int[] pixels = new int[8 * 8 * 4];

        skin.getData().getPixels(8, 8, 8, 8, pixels);

        for (int i = 0; i < 8 * 8; i++) {
            int inputIndex = i * 4;
            int outputIndex = i * 3;

            head[outputIndex] = (byte) pixels[inputIndex];
            head[outputIndex + 1] = (byte) pixels[inputIndex + 1];
            head[outputIndex + 2] = (byte) pixels[inputIndex + 2];
        }

        skin.getData().getPixels(40, 8, 8, 8, pixels);

        for (int i = 0; i < 8 * 8; i++) {
            int inputIndex = i * 4;
            int outputIndex = i * 3;

            if (pixels[inputIndex + 3] != 0) {
                head[outputIndex] = (byte) pixels[inputIndex];
                head[outputIndex + 1] = (byte) pixels[inputIndex + 1];
                head[outputIndex + 2] = (byte) pixels[inputIndex + 2];
            }
        }

        upload(BufferUtils.createByteBuffer(head.length).put(head));
    }

    public PlayerHeadTexture() {
        try (InputStream inputStream = mc.getResourceManager().getResource(MeteorClient.identifier("textures/steve.png")).get().getInputStream()) {
            ByteBuffer data = TextureUtil.readResource(inputStream);
            data.rewind();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                ByteBuffer image = STBImage.stbi_load_from_memory(data, width, height, comp, 3);
                upload(image);
                STBImage.stbi_image_free(image);
            }
            MemoryUtil.memFree(data);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void upload(ByteBuffer data) {
        Runnable action = () -> upload(8, 8, data, Texture.Format.RGB, Texture.Filter.Nearest, Texture.Filter.Nearest, false);
        if (RenderSystem.isOnRenderThread()) action.run();
        else RenderSystem.recordRenderCall(action::run);
    }
}
