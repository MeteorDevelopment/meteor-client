/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.*;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.texture.GlTexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

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
    private final STBTTPackContext packContext;
    private final ByteBuffer fontAtlasBuffer;
    private @Nullable ByteBuffer fileBuffer;

    public Font(FontFace fontFace, @NotNull ByteBuffer buffer, int height) {
        this.fontFace = fontFace;
        this.fileBuffer = buffer;
        this.height = height;

        // allocate data
        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        STBTruetype.stbtt_InitFont(fontInfo, buffer);

        texture = new Texture(size, size, TextureFormat.RED8, FilterMode.LINEAR, FilterMode.LINEAR);
        scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, height);

        // initialize font info & zero out texture
        fontAtlasBuffer = BufferUtils.createByteBuffer(size * size);

        packContext = STBTTPackContext.create();
        STBTruetype.stbtt_PackBegin(packContext, fontAtlasBuffer, size, size, 0 ,1);

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

    private void upload() {
        GlStateManager._bindTexture(((GlTexture) this.texture.getGlTexture()).getGlId());
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, size);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 1);
        GlStateManager._texSubImage2D(GlConst.GL_TEXTURE_2D, 0, 0, 0, size, size, GlConst.GL_RED, GlConst.GL_UNSIGNED_BYTE, MemoryUtil.memAddress0(this.fontAtlasBuffer));
    }

    // currently unused, but useful in the future hopefully
    private void regenerateAtlas() {
        if (this.fileBuffer == null) {
            this.fileBuffer = readFont(this.fontFace);
        }

        // Allocate buffers
        int chars = charMap.size();

        STBTTPackRange.Buffer packRange = STBTTPackRange.create(1);
        STBTTPackedchar.Buffer packedCharBuffer = STBTTPackedchar.create(chars);

        // create the pack range, populate with the specific packing ranges
        IntBuffer charBuffer = BufferUtils.createIntBuffer(chars);
        for (int c : charMap.keySet()) charBuffer.put(c);
        charMap.clear();
        charBuffer.flip();

        packRange.put(STBTTPackRange.create().set(height, 0, charBuffer, chars, packedCharBuffer, (byte) 0, (byte) 0));
        packRange.flip();

        // create and initialise packing context

        // pack and upload
        STBTruetype.stbtt_PackFontRanges(packContext, this.fileBuffer, 0, packRange);

        this.upload();

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
        this.fileBuffer = null;
    }

    private static ByteBuffer readFont(FontFace fontFace) {
        byte[] data = Utils.readBytes(fontFace.toStream());
        return BufferUtils.createByteBuffer(data.length).put(data).flip();
    }

    private CharData getCharData(int codepoint) {
        return charMap.computeIfAbsent(codepoint, c -> {
            if (this.fileBuffer == null) {
                this.fileBuffer = readFont(this.fontFace);
            }

            // allocate buffers
            STBTTPackRange.Buffer packRange = STBTTPackRange.create(1);
            STBTTPackedchar.Buffer packedCharBuffer = STBTTPackedchar.create(1);

            IntBuffer charBuffer = BufferUtils.createIntBuffer(1);
            charBuffer.put(codepoint);
            charBuffer.flip();

            packRange.put(STBTTPackRange.create().set(height, 0, charBuffer, 1, packedCharBuffer, (byte) 0, (byte) 0));
            packRange.flip();

            // pack and upload
            STBTruetype.stbtt_PackFontRanges(packContext, this.fileBuffer, 0, packRange);

            // update char data
            STBTTPackedchar packedChar = packedCharBuffer.get(0);

            float ipw = 1f / size; // pixel width and height
            float iph = 1f / size;

            return new CharData(
                packedChar.xoff(),
                packedChar.yoff(),
                packedChar.xoff2(),
                packedChar.yoff2(),
                packedChar.x0() * ipw,
                packedChar.y0() * iph,
                packedChar.x1() * ipw,
                packedChar.y1() * iph,
                packedChar.xadvance()
            );
        });
    }

    public double getWidth(String string, int length) {
        double width = 0;

        for (int i = 0; i < length; i++) {
            int cp = string.charAt(i);
            CharData c = this.getCharData(cp);
            width += c.xAdvance();
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
            CharData c = this.getCharData(cp);
            mesh.quad(
                mesh.vec2(x + c.x0() * scale, y + c.y0() * scale).vec2(c.u0(), c.v0()).color(color).next(),
                mesh.vec2(x + c.x0() * scale, y + c.y1() * scale).vec2(c.u0(), c.v1()).color(color).next(),
                mesh.vec2(x + c.x1() * scale, y + c.y1() * scale).vec2(c.u1(), c.v1()).color(color).next(),
                mesh.vec2(x + c.x1() * scale, y + c.y0() * scale).vec2(c.u1(), c.v0()).color(color).next()
            );

            x += c.xAdvance() * scale;
        }

        return x;
    }

    public Texture getTexture() {
        // flush updates
        if (this.fileBuffer != null) {
            this.upload();
            this.fileBuffer = null;
        }

        return this.texture;
    }

    public void close() {
        this.texture.close();
        STBTruetype.stbtt_PackEnd(this.packContext);
    }

    private record CharData(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, float xAdvance) {}
}
