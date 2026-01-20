/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.texture;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Arrays;

public record AnimatedImageData(Identifier imageId, int width, int height, int[] delays, int duration, GpuBuffer animBuffer) implements IImageData {
    @Override
    public boolean isAnimated() {
        return true;
    }

    public int getCurrentFrame() {
        long time = Util.getMeasuringTimeMs() % duration();
        int accumulated = 0;
        for (int i = 0; i < delays.length; i++) {
            accumulated += Math.max(delays[i],1);
            if (time < accumulated) {
                return i;
            }
        }
        return 0;
    }
}
