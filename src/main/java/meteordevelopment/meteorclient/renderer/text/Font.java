/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
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
import java.util.Objects;

public class Font implements AutoCloseable {
    private static final int ATLAS_SIZE = 2048;
    private static final int GLYPH_PADDING = 2;
    private static final int SPACE_CODE_POINT = ' ';

    public final Texture texture;

    private final int height;
    private final float scale;
    private final float ascent;
    private final ByteBuffer bitmap;
    private final List<FontData> fonts;
    private final Int2ObjectOpenHashMap<CharData> charMap = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<GlyphMetrics> metricsMap = new Int2ObjectOpenHashMap<>();
    private final IntOpenHashSet missingGlyphs = new IntOpenHashSet();
    private final IntOpenHashSet unpackableGlyphs = new IntOpenHashSet();

    private int packX;
    private int packY;
    private int rowHeight;
    private boolean dirty;
    private boolean closed;

    public Font(ByteBuffer buffer, int height) {
        this(List.of(buffer), height);
    }

    public Font(List<ByteBuffer> buffers, int height) {
        if (height <= 0) throw new IllegalArgumentException("Font height must be positive.");
        if (buffers.isEmpty()) throw new IllegalArgumentException("At least one font buffer is required.");

        this.height = height;

        List<FontData> loadedFonts = new ArrayList<>(buffers.size());
        FontData primaryFont = createFontData(buffers.get(0), height);
        if (primaryFont == null) throw new IllegalArgumentException("The primary font buffer is invalid.");

        loadedFonts.add(primaryFont);
        for (int i = 1; i < buffers.size(); i++) {
            FontData fallbackFont = createFontData(buffers.get(i), height);
            if (fallbackFont != null) loadedFonts.add(fallbackFont);
        }

        fonts = List.copyOf(loadedFonts);
        STBTTFontinfo fontInfo = primaryFont.info;

        // Allocate buffers
        bitmap = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE);
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
        if (!STBTruetype.stbtt_PackBegin(packContext, bitmap, ATLAS_SIZE, ATLAS_SIZE, 0, 1)) {
            throw new IllegalStateException("Failed to initialize the font atlas packer.");
        }
        STBTruetype.stbtt_PackSetSkipMissingCodepoints(packContext, true);

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
        try {
            // A false return value means at least one glyph did not fit. Successfully packed
            // glyphs are still usable; the remaining ones are loaded lazily below.
            STBTruetype.stbtt_PackFontRanges(packContext, primaryFont.buffer, 0, packRange);
        } finally {
            STBTruetype.stbtt_PackEnd(packContext);
        }

        // Create texture object and get font scale
        texture = new Texture(ATLAS_SIZE, ATLAS_SIZE, TextureFormat.RED8, FilterMode.LINEAR, FilterMode.LINEAR);
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
                int codePoint = j + offset;
                if (STBTruetype.stbtt_FindGlyphIndex(primaryFont.info, codePoint) == 0) continue;

                STBTTPackedchar packedChar = cbuf.get(j);
                if (!isPacked(packedChar)) continue;

                float ipw = 1f / ATLAS_SIZE; // pixel width and height
                float iph = 1f / ATLAS_SIZE;

                charMap.put(codePoint, new CharData(
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

        packY = Math.min(usedY + GLYPH_PADDING, ATLAS_SIZE);
    }

    public double getWidth(String string, int length) {
        ensureOpen();

        double width = 0;
        int end = Math.min(length, string.length());

        for (int i = 0; i < end; ) {
            int cp = codePointAt(string, i, end);
            width += getAdvance(cp);
            i += Character.charCount(cp);
        }

        return width;
    }

    public int getHeight() {
        return height;
    }

    public void uploadPendingGlyphs() {
        ensureOpen();
        if (!dirty) return;

        texture.upload(bitmap);
        dirty = false;
    }

    public double render(MeshBuilder mesh, String string, double x, double y, Color color, double scale) {
        ensureOpen();
        y += ascent * this.scale * scale;

        int length = string.length();
        mesh.ensureCapacity(length * 4, length * 6);

        for (int i = 0; i < length; ) {
            int cp = string.codePointAt(i);
            CharData c = getCharData(cp);

            if (c != null && c.hasBitmap()) {
                mesh.quad(
                    mesh.vec2(x + c.x0 * scale, y + c.y0 * scale).vec2(c.u0, c.v0).color(color).next(),
                    mesh.vec2(x + c.x0 * scale, y + c.y1 * scale).vec2(c.u0, c.v1).color(color).next(),
                    mesh.vec2(x + c.x1 * scale, y + c.y1 * scale).vec2(c.u1, c.v1).color(color).next(),
                    mesh.vec2(x + c.x1 * scale, y + c.y0 * scale).vec2(c.u1, c.v0).color(color).next()
                );
            }

            x += (c != null ? c.xAdvance : getAdvance(cp)) * scale;
            i += Character.charCount(cp);
        }

        return x;
    }

    @Override
    public void close() {
        if (closed) return;

        texture.close();
        closed = true;
    }

    private CharData getCharData(int cp) {
        CharData c = charMap.get(cp);
        if (c != null) return c;
        if (unpackableGlyphs.contains(cp)) return null;

        GlyphMetrics metrics = getGlyphMetrics(cp);
        if (metrics == null) return null;

        c = loadGlyph(cp, metrics);
        if (c != null) {
            charMap.put(cp, c);
        } else {
            unpackableGlyphs.add(cp);
        }

        return c;
    }

    private float getAdvance(int cp) {
        CharData data = charMap.get(cp);
        if (data != null) return data.xAdvance;

        GlyphMetrics metrics = getGlyphMetrics(cp);
        if (metrics != null) return metrics.xAdvance;

        if (cp != SPACE_CODE_POINT) return getAdvance(SPACE_CODE_POINT);
        return 0;
    }

    private GlyphMetrics getGlyphMetrics(int cp) {
        GlyphMetrics metrics = metricsMap.get(cp);
        if (metrics != null) return metrics;
        if (missingGlyphs.contains(cp)) return null;

        FontData font = findFont(cp);
        if (font == null) {
            missingGlyphs.add(cp);
            return null;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advanceWidth = stack.mallocInt(1);
            STBTruetype.stbtt_GetCodepointHMetrics(font.info, cp, advanceWidth, null);

            metrics = new GlyphMetrics(font, advanceWidth.get(0) * font.scale);
            metricsMap.put(cp, metrics);
            return metrics;
        }
    }

    private CharData loadGlyph(int cp, GlyphMetrics metrics) {
        FontData font = metrics.font;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer x0 = stack.mallocInt(1);
            IntBuffer y0 = stack.mallocInt(1);
            IntBuffer x1 = stack.mallocInt(1);
            IntBuffer y1 = stack.mallocInt(1);

            STBTruetype.stbtt_GetCodepointBitmapBox(font.info, cp, font.scale, font.scale, x0, y0, x1, y1);

            int bitmapWidth = x1.get(0) - x0.get(0);
            int bitmapHeight = y1.get(0) - y0.get(0);

            if (bitmapWidth <= 0 || bitmapHeight <= 0) {
                return new CharData(0, 0, 0, 0, 0, 0, 0, 0, metrics.xAdvance);
            }

            if (!reserve(bitmapWidth, bitmapHeight)) return null;

            int glyphX = packX + GLYPH_PADDING;
            int glyphY = packY + GLYPH_PADDING;

            ByteBuffer glyphTarget = bitmap.duplicate();
            glyphTarget.position(glyphY * ATLAS_SIZE + glyphX);
            STBTruetype.stbtt_MakeCodepointBitmap(font.info, glyphTarget.slice(), bitmapWidth, bitmapHeight, ATLAS_SIZE, font.scale, font.scale, cp);

            float ipw = 1f / ATLAS_SIZE;
            float iph = 1f / ATLAS_SIZE;

            CharData charData = new CharData(
                x0.get(0),
                y0.get(0),
                x1.get(0),
                y1.get(0),
                glyphX * ipw,
                glyphY * iph,
                (glyphX + bitmapWidth) * ipw,
                (glyphY + bitmapHeight) * iph,
                metrics.xAdvance
            );

            packX += bitmapWidth + GLYPH_PADDING * 2;
            rowHeight = Math.max(rowHeight, bitmapHeight + GLYPH_PADDING * 2);

            dirty = true;
            return charData;
        }
    }

    private boolean reserve(int bitmapWidth, int bitmapHeight) {
        int requiredWidth = bitmapWidth + GLYPH_PADDING * 2;
        int requiredHeight = bitmapHeight + GLYPH_PADDING * 2;

        if (requiredWidth > ATLAS_SIZE || requiredHeight > ATLAS_SIZE) return false;

        if (packX + requiredWidth > ATLAS_SIZE) {
            packX = 0;
            packY += rowHeight;
            rowHeight = 0;
        }

        if (packY + requiredHeight > ATLAS_SIZE) return false;

        return true;
    }

    private FontData findFont(int cp) {
        for (FontData font : fonts) {
            if (STBTruetype.stbtt_FindGlyphIndex(font.info, cp) != 0) return font;
        }

        return null;
    }

    private static FontData createFontData(ByteBuffer source, int height) {
        ByteBuffer buffer = Objects.requireNonNull(source, "Font buffer cannot be null.").duplicate();
        STBTTFontinfo info = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(info, buffer)) return null;

        return new FontData(buffer, info, STBTruetype.stbtt_ScaleForPixelHeight(info, height));
    }

    private static boolean isPacked(STBTTPackedchar packedChar) {
        return packedChar.x0() != packedChar.x1()
            || packedChar.y0() != packedChar.y1()
            || packedChar.xadvance() != 0;
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Font has already been closed.");
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

    private record GlyphMetrics(FontData font, float xAdvance) {}

    private record CharData(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, float xAdvance) {
        private boolean hasBitmap() {
            return x0 != x1 && y0 != y1;
        }
    }
}
