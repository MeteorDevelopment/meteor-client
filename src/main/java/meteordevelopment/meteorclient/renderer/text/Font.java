/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.*;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Font {
    private static final int size = 2048;
    private final Texture texture;
    private final FontFace fontFace;
    private final int height;
    private final float scale;
    private final float ascent;
    private final Int2ObjectOpenHashMap<CharData> charMap = new Int2ObjectOpenHashMap<>();
    private final Int2IntMap pending = new Int2IntOpenHashMap();
    private @Nullable STBTTFontinfo fontInfo;
    private @Nullable ByteBuffer fontBuffer;

    public Font(FontFace fontFace, ByteBuffer buffer, int height) {
        this.fontFace = fontFace;
        this.fontBuffer = buffer;
        this.height = height;

        // Initialize font
        STBTTFontinfo fontInfo = this.getOrCreateFontInfo();
        texture = new Texture(size, size, TextureFormat.RED8, FilterMode.LINEAR, FilterMode.LINEAR);
        scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, height);

        // Get font vertical ascent
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
            this.ascent = ascent.get(0);
        }
    }

    public Font(FontFace fontFace, int height) {
        this(fontFace, readFont(fontFace), height);
    }

    private void renderAndUploadAtlas() {
        // Allocate buffers
        int chars = charMap.size() + pending.size();

        STBTTPackRange.Buffer packRange = STBTTPackRange.create(1);
        STBTTPackedchar.Buffer packedCharBuffer = STBTTPackedchar.create(chars);

        // create the pack range, populate with the specific packing ranges
        IntBuffer charBuffer = BufferUtils.createIntBuffer(chars);
        for (int c : charMap.keySet()) charBuffer.put(c);
        charMap.clear();
        for (int c : pending.keySet()) charBuffer.put(c);
        pending.clear();
        charBuffer.flip();

        packRange.put(STBTTPackRange.create().set(height, 0, charBuffer, chars, packedCharBuffer, (byte) 0, (byte) 0));
        packRange.flip();

        // create and initialise packing context
        ByteBuffer bitmap = BufferUtils.createByteBuffer(size * size);

        STBTTPackContext packContext = STBTTPackContext.create();
        STBTruetype.stbtt_PackBegin(packContext, bitmap, size, size, 0 ,1);

        // pack and upload
        STBTruetype.stbtt_PackFontRanges(packContext, this.fontBuffer, 0, packRange);
        STBTruetype.stbtt_PackEnd(packContext);

        texture.upload(bitmap);

        // update char data
        for (int i = 0; i < chars; i++) {
            int codepoint = charBuffer.get(i);
            STBTTPackedchar packedChar = packedCharBuffer.get(i);

            float ipw = 1f / size; // pixel width and height
            float iph = 1f / size;

            charMap.put(codepoint, new CharData(
                packedChar.xoff(),
                packedChar.yoff(),
                packedChar.xoff2(),
                packedChar.yoff2(),
                packedChar.x0() * ipw,
                packedChar.y0() * iph,
                packedChar.x1() * ipw,
                packedChar.y1() * iph,
                packedChar.xadvance()
            ));
        }

        // clear
        this.fontInfo = null;
        this.fontBuffer = null;
    }

    private STBTTFontinfo getOrCreateFontInfo() {
        if (this.fontInfo != null) {
            return this.fontInfo;
        }

        if (this.fontBuffer == null) {
            this.fontBuffer = readFont(this.fontFace);
        }

        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        STBTruetype.stbtt_InitFont(fontInfo, fontBuffer);

        return this.fontInfo = fontInfo;
    }

    private static ByteBuffer readFont(FontFace fontFace) {
        byte[] data = Utils.readBytes(fontFace.toStream());
        return BufferUtils.createByteBuffer(data.length).put(data).flip();
    }

    private int addCharacter(int codepoint) {
        return pending.computeIfAbsent(codepoint, c -> {
            STBTTFontinfo fontInfo = this.getOrCreateFontInfo();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer advance = stack.mallocInt(1);
                STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, c, advance, null);
                return advance.get();
            }
        });
    }

    public double getWidth(String string, int length) {
        double width = 0;

        for (int i = 0; i < length; i++) {
            int cp = string.charAt(i);
            @Nullable CharData c = charMap.get(cp);
            if (c != null) {
                width += c.xAdvance;
            } else {
                int advance = addCharacter(cp);
                width += advance * this.scale;
            }
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

        for (int i = 0; i < length; i++) {
            int cp = string.charAt(i);
            @Nullable CharData c = charMap.get(cp);
            if (c != null) {
                mesh.quad(
                    mesh.vec2(x + c.x0 * scale, y + c.y0 * scale).vec2(c.u0, c.v0).color(color).next(),
                    mesh.vec2(x + c.x0 * scale, y + c.y1 * scale).vec2(c.u0, c.v1).color(color).next(),
                    mesh.vec2(x + c.x1 * scale, y + c.y1 * scale).vec2(c.u1, c.v1).color(color).next(),
                    mesh.vec2(x + c.x1 * scale, y + c.y0 * scale).vec2(c.u1, c.v0).color(color).next()
                );

                x += c.xAdvance * scale;
            } else {
                int advance = addCharacter(cp);
                x += advance * this.scale * scale;
            }
        }

        return x;
    }

    public Texture getTexture() {
        if (fontInfo != null) {
            renderAndUploadAtlas();
        }

        return this.texture;
    }

    public void close() {
        this.texture.close();
    }

    private record CharData(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, float xAdvance) {}
}
