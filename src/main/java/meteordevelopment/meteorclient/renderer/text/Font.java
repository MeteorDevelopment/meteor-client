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
import java.util.ArrayList;
import java.util.List;

public class Font {
    public final Texture texture;
    private final int height;
    private final float scale;
    private final float ascent;
    private final ByteBuffer bitmap;
    private final List<FontData> fonts = new ArrayList<>();
    private final Int2ObjectOpenHashMap<CharData> charMap = new Int2ObjectOpenHashMap<>();
    private static final int size = 2048;
    private static final int padding = 2;

    private int packX;
    private int packY;
    private int rowHeight;
    private boolean textureFull;
    private boolean dirty;

    public Font(ByteBuffer buffer, int height) {
        this(List.of(buffer), height);
    }

    public Font(List<ByteBuffer> buffers, int height) {
        this.height = height;

        // Initialize fonts
        for (ByteBuffer buffer : buffers) {
            STBTTFontinfo fontInfo = STBTTFontinfo.create();
            if (STBTruetype.stbtt_InitFont(fontInfo, buffer)) {
                fonts.add(new FontData(buffer, fontInfo, STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, height)));
            }
        }

        if (fonts.isEmpty()) {
            throw new IllegalArgumentException("No valid fonts were provided.");
        }

        FontData primaryFont = fonts.get(0);
        STBTTFontinfo fontInfo = primaryFont.info;

        // Allocate buffers
        bitmap = BufferUtils.createByteBuffer(size * size);
        STBTTPackedchar.Buffer[] cdata = {
            STBTTPackedchar.create(95), // Basic Latin
            STBTTPackedchar.create(96), // Latin 1 Supplement
            STBTTPackedchar.create(128), // Latin Extended-A
            STBTTPackedchar.create(144), // Greek and Coptic
            STBTTPackedchar.create(256), // Cyrillic
            STBTTPackedchar.create(1) // infinity symbol
        };

        // create and initialise packing context
        STBTTPackContext packContext = STBTTPackContext.create();
        STBTruetype.stbtt_PackBegin(packContext, bitmap, size, size, 0 ,1);

        // create the pack range, populate with the specific packing ranges
        STBTTPackRange.Buffer packRange = STBTTPackRange.create(cdata.length);
        packRange.put(STBTTPackRange.create().set(height, 32, null, 95, cdata[0], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 160, null, 96, cdata[1], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 256, null, 128, cdata[2], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 880, null, 144, cdata[3], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 1024, null, 256, cdata[4], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 8734, null, 1, cdata[5], (byte) 2, (byte) 2)); // lol
        packRange.flip();

        // write and finish
        STBTruetype.stbtt_PackFontRanges(packContext, primaryFont.buffer, 0, packRange);
        STBTruetype.stbtt_PackEnd(packContext);

        // Create texture object and get font scale
        texture = new Texture(size, size, TextureFormat.RED8, FilterMode.LINEAR, FilterMode.LINEAR);
        texture.upload(bitmap);
        scale = primaryFont.scale;

        // Get font vertical ascent
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
            this.ascent = ascent.get(0);
        }

        int usedY = 0;
        for (int i = 0; i < cdata.length; i++) {
            STBTTPackedchar.Buffer cbuf = cdata[i];
            int offset = packRange.get(i).first_unicode_codepoint_in_range();

            for (int j = 0; j < cbuf.capacity(); j++) {
                STBTTPackedchar packedChar = cbuf.get(j);

                float ipw = 1f / size; // pixel width and height
                float iph = 1f / size;

                charMap.put(j + offset, new CharData(
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

                usedY = Math.max(usedY, packedChar.y1());
            }
        }

        packY = Math.min(usedY + padding, size);
    }

    public double getWidth(String string, int length) {
        double width = 0;
        int end = Math.min(length, string.length());

        for (int i = 0; i < end; ) {
            int cp = codePointAt(string, i, end);
            CharData c = getCharData(cp);
            if (c == null) c = charMap.get(32);

            width += c.xAdvance;
            i += Character.charCount(cp);
        }

        return width;
    }

    public boolean hasGlyphs(String string, int length) {
        int end = Math.min(length, string.length());

        for (int i = 0; i < end; ) {
            int cp = codePointAt(string, i, end);
            if (getCharData(cp) == null) return false;

            i += Character.charCount(cp);
        }

        return true;
    }

    public int getHeight() {
        return height;
    }

    public void uploadPendingGlyphs() {
        if (!dirty) return;

        texture.upload(bitmap);
        dirty = false;
    }

    public double render(MeshBuilder mesh, String string, double x, double y, Color color, double scale) {
        y += ascent * this.scale * scale;

        int length = string.length();
        mesh.ensureCapacity(length * 4, length * 6);

        for (int i = 0; i < length; ) {
            int cp = string.codePointAt(i);
            CharData c = getCharData(cp);
            if (c == null) c = charMap.get(32);

            mesh.quad(
                mesh.vec2(x + c.x0 * scale, y + c.y0 * scale).vec2(c.u0, c.v0).color(color).next(),
                mesh.vec2(x + c.x0 * scale, y + c.y1 * scale).vec2(c.u0, c.v1).color(color).next(),
                mesh.vec2(x + c.x1 * scale, y + c.y1 * scale).vec2(c.u1, c.v1).color(color).next(),
                mesh.vec2(x + c.x1 * scale, y + c.y0 * scale).vec2(c.u1, c.v0).color(color).next()
            );

            x += c.xAdvance * scale;
            i += Character.charCount(cp);
        }

        return x;
    }

    private CharData getCharData(int cp) {
        CharData c = charMap.get(cp);
        if (c != null) return c;

        c = loadGlyph(cp);
        if (c != null) charMap.put(cp, c);

        return c;
    }

    private CharData loadGlyph(int cp) {
        if (textureFull) return null;

        FontData font = findFont(cp);
        if (font == null) return null;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advanceWidth = stack.mallocInt(1);
            IntBuffer x0 = stack.mallocInt(1);
            IntBuffer y0 = stack.mallocInt(1);
            IntBuffer x1 = stack.mallocInt(1);
            IntBuffer y1 = stack.mallocInt(1);

            STBTruetype.stbtt_GetCodepointHMetrics(font.info, cp, advanceWidth, null);
            STBTruetype.stbtt_GetCodepointBitmapBox(font.info, cp, font.scale, font.scale, x0, y0, x1, y1);

            int bitmapWidth = x1.get(0) - x0.get(0);
            int bitmapHeight = y1.get(0) - y0.get(0);
            float xAdvance = advanceWidth.get(0) * font.scale;

            if (bitmapWidth <= 0 || bitmapHeight <= 0) {
                return new CharData(0, 0, 0, 0, 0, 0, 0, 0, xAdvance);
            }

            if (!reserve(bitmapWidth, bitmapHeight)) return null;

            int glyphX = packX + padding;
            int glyphY = packY + padding;
            ByteBuffer glyphBitmap = BufferUtils.createByteBuffer(bitmapWidth * bitmapHeight);

            STBTruetype.stbtt_MakeCodepointBitmap(font.info, glyphBitmap, bitmapWidth, bitmapHeight, bitmapWidth, font.scale, font.scale, cp);

            for (int row = 0; row < bitmapHeight; row++) {
                int src = row * bitmapWidth;
                int dst = (glyphY + row) * size + glyphX;

                for (int col = 0; col < bitmapWidth; col++) {
                    bitmap.put(dst + col, glyphBitmap.get(src + col));
                }
            }

            float ipw = 1f / size;
            float iph = 1f / size;

            CharData charData = new CharData(
                x0.get(0),
                y0.get(0),
                x1.get(0),
                y1.get(0),
                glyphX * ipw,
                glyphY * iph,
                (glyphX + bitmapWidth) * ipw,
                (glyphY + bitmapHeight) * iph,
                xAdvance
            );

            packX += bitmapWidth + padding * 2;
            rowHeight = Math.max(rowHeight, bitmapHeight + padding * 2);

            dirty = true;
            return charData;
        }
    }

    private boolean reserve(int bitmapWidth, int bitmapHeight) {
        int requiredWidth = bitmapWidth + padding * 2;
        int requiredHeight = bitmapHeight + padding * 2;

        if (requiredWidth > size || requiredHeight > size) {
            textureFull = true;
            return false;
        }

        if (packX + requiredWidth > size) {
            packX = 0;
            packY += rowHeight;
            rowHeight = 0;
        }

        if (packY + requiredHeight > size) {
            textureFull = true;
            return false;
        }

        return true;
    }

    private FontData findFont(int cp) {
        for (FontData font : fonts) {
            if (STBTruetype.stbtt_FindGlyphIndex(font.info, cp) != 0) return font;
        }

        return null;
    }

    private static int codePointAt(String string, int index, int end) {
        char c = string.charAt(index);

        if (Character.isHighSurrogate(c) && index + 1 < end) {
            char c2 = string.charAt(index + 1);
            if (Character.isLowSurrogate(c2)) return Character.toCodePoint(c, c2);
        }

        return c;
    }

    private record FontData(ByteBuffer buffer, STBTTFontinfo info, float scale) {}

    private record CharData(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, float xAdvance) {}
}
