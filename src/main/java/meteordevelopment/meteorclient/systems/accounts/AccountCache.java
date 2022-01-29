/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.accounts;

import com.mojang.blaze3d.platform.TextureUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AccountCache implements ISerializable<AccountCache> {
    private static Texture STEVE_HEAD;

    public String username = "";
    public String uuid = "";

    private Texture headTexture;

    public Texture getHeadTexture() {
        return headTexture != null ? headTexture : STEVE_HEAD;
    }

    public boolean shouldRotateHeadTexture() {
        return headTexture != null;
    }

    public boolean loadHead(String url) {
        try {
            BufferedImage skin = ImageIO.read(Http.get(url).sendInputStream());
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

            headTexture = new Texture(8, 8, head, Texture.Format.RGB, Texture.Filter.Nearest, Texture.Filter.Nearest);
            return true;
        } catch (IOException e) {
            MeteorClient.LOG.error("Failed to read skin url (" + url + ").");
            return false;
        }
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putString("username", username);
        tag.putString("uuid", uuid);

        return tag;
    }

    @Override
    public AccountCache fromTag(NbtCompound tag) {
        if (!tag.contains("username") || !tag.contains("uuid")) throw new NbtException();

        username = tag.getString("username");
        uuid = tag.getString("uuid");

        return this;
    }

    public static void loadSteveHead() {
        try {
            ByteBuffer data = TextureUtil.readResource(mc.getResourceManager().getResource(new Identifier(MeteorClient.MOD_ID, "textures/steve.png")).getInputStream());
            data.rewind();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                ByteBuffer image = STBImage.stbi_load_from_memory(data, width, height, comp, 3);

                STEVE_HEAD = new Texture();
                STEVE_HEAD.upload(width.get(0), height.get(0), image, Texture.Format.RGB, Texture.Filter.Nearest, Texture.Filter.Nearest, false);

                STBImage.stbi_image_free(image);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
