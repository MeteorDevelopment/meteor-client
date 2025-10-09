/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc.texture;

import net.minecraft.client.texture.NativeImage;

import java.util.List;

public class ImageData {
    String name;
    NativeImage texture;
    public int width;
    public int height;
    public int canvasWidth;
    public int canvasHeight;
    public int framesPerColumn;
    int totalFrames;
    public List<Integer> delays;

    public ImageData(String name) {
        this.name = name;
    }

    public int getColumns(){
        return (int) Math.ceil(totalFrames / (double) framesPerColumn);
    }
}
