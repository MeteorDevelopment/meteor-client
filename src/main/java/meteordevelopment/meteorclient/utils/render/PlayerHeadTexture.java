package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.misc.MeteorIdentifier;
import meteordevelopment.meteorclient.utils.network.Http;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerHeadTexture extends Texture {
    private boolean needsRotate;

    public PlayerHeadTexture(String url) {
        BufferedImage skin;
        try {
            skin = ImageIO.read(Http.get(url).sendInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        byte[] head = new byte[8 * 8 * 3];
        int[] pixel = new int[4];

        int i = 0;
        for (int x = 8; x < 16; x++) {
            for (int y = 8; y < 16; y++) {
                skin.getData().getPixel(x, y, pixel);

                for (int j = 0; j < 3; j++) {
                    head[i] = (byte) pixel[j];
                    i++;
                }
            }
        }

        i = 0;
        for (int x = 40; x < 48; x++) {
            for (int y = 8; y < 16; y++) {
                skin.getData().getPixel(x, y, pixel);

                if (pixel[3] != 0) {
                    for (int j = 0; j < 3; j++) {
                        head[i] = (byte) pixel[j];
                        i++;
                    }
                }
                else i += 3;
            }
        }

        upload(BufferUtils.createByteBuffer(head.length).put(head));

        needsRotate = true;
    }

    public PlayerHeadTexture() {
        try {
            ByteBuffer data = TextureUtil.readResource(mc.getResourceManager().getResource(new MeteorIdentifier("textures/steve.png")).get().getInputStream());
            data.rewind();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                ByteBuffer image = STBImage.stbi_load_from_memory(data, width, height, comp, 3);
                upload(image);
                STBImage.stbi_image_free(image);
            }
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

    public boolean needsRotate() {
        return needsRotate;
    }
}
