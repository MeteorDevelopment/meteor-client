/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Font {
    public final Texture texture;
    private final int height;
    private final float scale;
    private final float ascent;
    private final Int2ObjectOpenHashMap<CharData> charMap = new Int2ObjectOpenHashMap<>();
    private static final int size = 2048;

    private final STBTTFontinfo fontInfo;
    private final ByteBuffer fontBuffer;
    private final ByteBuffer bitmap;
    private int currentX = 0;
    private int currentY = 0;
    private int rowHeight = 0;
    private static final int PADDING = 2;

    // track dirty regions for partial texture updates
    private int dirtyMinX = Integer.MAX_VALUE;
    private int dirtyMinY = Integer.MAX_VALUE;
    private int dirtyMaxX = 0;
    private int dirtyMaxY = 0;
    private boolean needsTextureUpdate = false;

    public Font(ByteBuffer buffer, int height) {
        this.height = height;
        this.fontBuffer = buffer;

        // init
        fontInfo = STBTTFontinfo.create();
        STBTruetype.stbtt_InitFont(fontInfo, buffer);

        bitmap = BufferUtils.createByteBuffer(size * size);

        texture = new Texture(size, size, TextureFormat.RED8, FilterMode.LINEAR, FilterMode.LINEAR);
        scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, height);

        // get font vertical ascent
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
            this.ascent = ascent.get(0);
        }

        // preload only basic ascii
        preloadCharacterRange(32, 126);
        preloadCharacterRange(160, 255);

        texture.upload(bitmap);
    }

    private void preloadCharacterRange(int start, int end) {
        for (int cp = start; cp <= end; cp++) {
            loadCharacter(cp);
        }
    }

    private void loadCharacter(int codepoint) {
        if (charMap.containsKey(codepoint)) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer xoff = stack.mallocInt(1);
            IntBuffer yoff = stack.mallocInt(1);

            ByteBuffer charBitmap = STBTruetype.stbtt_GetCodepointBitmap(
                fontInfo, scale, scale, codepoint, width, height, xoff, yoff
            );

            if (charBitmap == null) {
                if (codepoint != 32 && charMap.containsKey(32)) {
                    charMap.put(codepoint, charMap.get(32));
                }
                return;
            }

            int w = width.get(0);
            int h = height.get(0);

            if (currentX + w + PADDING > size) {
                currentX = 0;
                currentY += rowHeight + PADDING;
                rowHeight = 0;
            }

            // if we're out of texture space
            if (currentY + h + PADDING > size) {
                STBTruetype.stbtt_FreeBitmap(charBitmap, 0L);
                return;
            }

            // cp char bitmap to main bitmap
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int srcIdx = y * w + x;
                    int dstIdx = (currentY + y) * size + (currentX + x);
                    bitmap.put(dstIdx, charBitmap.get(srcIdx));
                }
            }

            // Update dirty region bounds
            dirtyMinX = Math.min(dirtyMinX, currentX);
            dirtyMinY = Math.min(dirtyMinY, currentY);
            dirtyMaxX = Math.max(dirtyMaxX, currentX + w);
            dirtyMaxY = Math.max(dirtyMaxY, currentY + h);

            IntBuffer advanceWidth = stack.mallocInt(1);
            STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, codepoint, advanceWidth, null);

            float ipw = 1f / size;
            float iph = 1f / size;

            float u0 = currentX * ipw;
            float v0 = currentY * iph;
            float u1 = (currentX + w) * ipw;
            float v1 = (currentY + h) * iph;

            // save char data
            charMap.put(codepoint, new CharData(
                xoff.get(0),
                yoff.get(0),
                xoff.get(0) + w,
                yoff.get(0) + h,
                u0, v0, u1, v1,
                advanceWidth.get(0) * scale
            ));

            currentX += w + PADDING;
            rowHeight = Math.max(rowHeight, h);

            STBTruetype.stbtt_FreeBitmap(charBitmap, 0L);
        }
    }

    public double getWidth(String string, int length) {
        double width = 0;

        for (int i = 0; i < length; i++) {
            int cp = string.charAt(i);

            // load not loaded char
            if (!charMap.containsKey(cp)) {
                loadCharacter(cp);
            }

            CharData c = charMap.get(cp);
            if (c == null) {
                c = charMap.get(32);  // fallback
                if (c == null) continue;
            }

            width += c.xAdvance;
        }

        return width;
    }

    public int getHeight() {
        return height;
    }

    public double render(MeshBuilder mesh, String string, double x, double y, Color color, double scale) {
        y += ascent * this.scale * scale;

        int length = string.length();
        mesh.ensureCapacity(length * 4, length * 6);

        // load all missing chars
        for (int i = 0; i < length; i++) {
            int cp = string.charAt(i);
            if (!charMap.containsKey(cp)) {
                loadCharacter(cp);
                needsTextureUpdate = true;
            }
        }

        // render chars
        for (int i = 0; i < length; i++) {
            int cp = string.charAt(i);
            CharData c = charMap.get(cp);
            if (c == null) {
                c = charMap.get(32);
                if (c == null) continue;
            }

            mesh.quad(
                mesh.vec2(x + c.x0 * scale, y + c.y0 * scale).vec2(c.u0, c.v0).color(color).next(),
                mesh.vec2(x + c.x0 * scale, y + c.y1 * scale).vec2(c.u0, c.v1).color(color).next(),
                mesh.vec2(x + c.x1 * scale, y + c.y1 * scale).vec2(c.u1, c.v1).color(color).next(),
                mesh.vec2(x + c.x1 * scale, y + c.y0 * scale).vec2(c.u1, c.v0).color(color).next()
            );

            x += c.xAdvance * scale;
        }

        return x;
    }

    public void updateTextureIfNeeded() {
        if (needsTextureUpdate) {
            if (dirtyMinX < dirtyMaxX && dirtyMinY < dirtyMaxY) {
                int width = dirtyMaxX - dirtyMinX;
                int height = dirtyMaxY - dirtyMinY;
                texture.uploadRegion(bitmap, dirtyMinX, dirtyMinY, width, height);
            } else {
                texture.upload(bitmap);
            }

            needsTextureUpdate = false;

            // reset dirty region
            dirtyMinX = Integer.MAX_VALUE;
            dirtyMinY = Integer.MAX_VALUE;
            dirtyMaxX = 0;
            dirtyMaxY = 0;
        }
    }

    private record CharData(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, float xAdvance) {}
}
