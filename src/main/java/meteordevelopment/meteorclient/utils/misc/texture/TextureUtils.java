/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.texture;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.awt.image.BufferedImage;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.*;

public class TextureUtils {
    /**
     * Registers the texture from an imageData object parsing for invalid chars.
     * @return Identifier of the registered texture.
     */
    public static Identifier registerTexture(ImageData imageData) {
        String name = imageData.name.toLowerCase().replaceAll("[^a-z0-9/._-]","_");
        Identifier identifier = Identifier.of(MOD_ID,name);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> imageData.name, imageData.texture);
        mc.getTextureManager().registerTexture(identifier, texture);
        return identifier;
    }

    /**
     * Registers the texture from an imageData object parsing for invalid chars.
     * @return Identifier of the registered texture.
     */
    public static NativeImage bufferedToNative(BufferedImage canvas) {
        NativeImage tex = new NativeImage(canvas.getWidth(), canvas.getHeight(), true);
        for (int x = 0; x < canvas.getWidth(); x++) {
            for (int y = 0; y < canvas.getHeight(); y++) {
                tex.setColorArgb(x, y, canvas.getRGB(x, y));
            }
        }
        return tex;
    }

    /**
     * Calculates the frame of an animated texture taking into account the delays in centiseconds and the system time.
     */
    public static int getCurrentAnimationFrame(List<Integer> delays) {
        int total = 0;
        long time = Util.getMeasuringTimeMs() % delays.stream().mapToInt(d -> d * 10).sum();
        for (int i = 0; i < delays.size(); i++) {
            total += delays.get(i) * 10;
            if (time < total) return i;
        }
        return 0;
    }
}
