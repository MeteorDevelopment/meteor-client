/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.accounts;

import minegame159.meteorclient.utils.ByteTexture;
import minegame159.meteorclient.utils.ISerializable;
import minegame159.meteorclient.utils.NbtException;
import net.minecraft.nbt.CompoundTag;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class AccountCache implements ISerializable<AccountCache> {
    private static ByteTexture STEVE;

    private final boolean steve;

    public String username = "";
    public String uuid = "";

    private ByteTexture headTexture;

    public AccountCache(boolean steve) {
        this.steve = steve;
    }

    public ByteTexture getHeadTexture() {
        if (steve) return STEVE;
        return headTexture;
    }

    public boolean makeHead(String skinUrl) {
        if (steve && STEVE != null) return true;

        try {
            BufferedImage skin = ImageIO.read(new URL(skinUrl));
            byte[] head = new byte[8 * 8 * 3];
            int[] pixel = new int[4];

            // Head
            int i = 0;
            for (int x = 8; x < 8 + 8; x++) {
                for (int y = 8; y < 8 + 8; y++) {
                    skin.getData().getPixel(x, y, pixel);

                    for (int j = 0; j < 3; j++) {
                        head[i] = (byte) pixel[j];
                        i++;
                    }
                }
            }

            // Hair or idk how to call it
            i = 0;
            for (int x = 40; x < 40 + 8; x++) {
                for (int y = 8; y < 8 + 8; y++) {
                    skin.getData().getPixel(x, y, pixel);

                    int a = pixel[3];
                    for (int j = 0; j < 3; j++) {
                        if (a == 255) head[i] = (byte) pixel[j];
                        i++;
                    }
                }
            }

            if (steve) {
                STEVE = new ByteTexture(8, 8, head, false);
            } else {
                headTexture = new ByteTexture(8, 8, head, false);
            }

            return true;
        } catch (IOException e) {
            System.out.println("Failed to read skin url. (" + skinUrl + ")");
            return false;
        }
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("username", username);
        tag.putString("uuid", uuid);

        return tag;
    }

    @Override
    public AccountCache fromTag(CompoundTag tag) {
        if (!tag.contains("username") || !tag.contains("uuid")) throw new NbtException();

        username = tag.getString("username");
        uuid = tag.getString("uuid");

        return this;
    }
}
