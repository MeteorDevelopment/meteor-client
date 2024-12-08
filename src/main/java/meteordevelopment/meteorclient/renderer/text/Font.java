/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.renderer.text;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.renderer.Mesh;
import meteordevelopment.meteorclient.utils.render.ByteTexture;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;


public class Font {
    public ByteTexture texture;
    private final int height;
    private final float scale;
    private final float ascent;
    private final Int2ObjectOpenHashMap<CharData> charMap = new Int2ObjectOpenHashMap<>();
    private static final int size = 2048;

    private final ByteBuffer buffer;
    private final STBTTFontinfo fontInfo;
    private final ByteBuffer bitmap;
    private final STBTTPackContext packContext;
    private final Int2ObjectOpenHashMap<STBTTPackedchar> packedChars = new Int2ObjectOpenHashMap<>();

    private  long loadTimer = 0;
    private int loadCount=0;
    private final int loadSpeedLimit = 7;
    //The number of string that can be loaded per 100ms

    public Font(ByteBuffer buffer, int height) {
        this.buffer = buffer;
        this.height = height;

        // Initialize font
        fontInfo = STBTTFontinfo.create();
        STBTruetype.stbtt_InitFont(fontInfo, buffer);

        // Allocate buffers
        bitmap = BufferUtils.createByteBuffer(size * size);

        // create and initialise packing context
        packContext = STBTTPackContext.create();
        STBTruetype.stbtt_PackBegin(packContext, bitmap, size, size, 0, 1);

        // Create texture object and get font scale
        texture = new ByteTexture(size, size, bitmap, ByteTexture.Format.A, ByteTexture.Filter.Linear, ByteTexture.Filter.Linear);
        scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, height);

        // Get font vertical ascent
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
            this.ascent = ascent.get(0);
        }

        // Preload basic ASCII characters
        preloadAsciiCharacters();
    }

    private void preloadAsciiCharacters() {
        STBTTPackedchar.Buffer cdata = STBTTPackedchar.create(128); // Basic Latin

        // create the pack range
        STBTTPackRange.Buffer packRange = STBTTPackRange.create(1);
        packRange.put(STBTTPackRange.create().set(height, 32, null, 128, cdata, (byte) 2, (byte) 2));
        packRange.flip();

        // pack font ranges
        STBTruetype.stbtt_PackFontRanges(packContext, buffer, 0, packRange);

        // Load character data into charMap
        for (int i = 0; i < cdata.capacity(); i++) {
            STBTTPackedchar packedChar = cdata.get(i);
            putCharData(i + 32, packedChar);
        }

        // Create texture
        createTexture();
    }

    private void loadCharacter(List<Integer> codePoints) {
        if (System.currentTimeMillis()-loadTimer>100){
            loadTimer = System.currentTimeMillis();
            loadCount =0;
        }
        if (loadCount>=loadSpeedLimit)return;
        //Limit the load speed to avoid blocking the rendering thread
        for (Integer codePoint : codePoints) {
            loadCharacter(codePoint);
        }
        // Re-create texture
        createTexture();
        loadCount++;
    }

    private void loadCharacter(int codePoint) {
        if (charMap.containsKey(codePoint)) return;

        STBTTPackedchar.Buffer cdata = STBTTPackedchar.create(1);

        // create the pack range
        STBTTPackRange.Buffer packRange = STBTTPackRange.create(1);
        packRange.put(STBTTPackRange.create().set(height, codePoint, null, 1, cdata, (byte) 2, (byte) 2));
        packRange.flip();

        // pack font ranges
        STBTruetype.stbtt_PackFontRanges(packContext, buffer, 0, packRange);

        STBTTPackedchar packedChar = cdata.get(0);
        putCharData(codePoint, packedChar);
        packedChars.put(codePoint, packedChar);
    }

    private void putCharData(int codePoint, STBTTPackedchar packedChar) {
        float ipw = 1f / size; // pixel width and height
        float iph = 1f / size;
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
    }

    private void createTexture() {
        texture.upload(size, size, bitmap, ByteTexture.Format.A, ByteTexture.Filter.Linear, ByteTexture.Filter.Linear);
    }

    public double getWidth(String string, int length) {
        double width = 0;
        if (tryLoadString(string)) {
            return width;
        }
        for (int i = 0; i < length; i++) {
            int cp = string.charAt(i);
            CharData c = charMap.get(cp);
            if (c == null) {
                continue;
            }
            width += c.xAdvance;
        }
        return width;
    }

    public int getHeight() {
        return height;
    }
    private boolean tryLoadString(String s){
        boolean isLoading = false;
        List<Integer> charPoints = null;
        for (int i = 0; i < s.length(); i++) {
            int cp = s.charAt(i);
            CharData c = charMap.get(cp);
            if (c==null){
                if (charPoints == null) charPoints = new ArrayList<>();
                charPoints.add(cp);
                isLoading  = true;
            }
        }
        if (charPoints != null) {
            loadCharacter(charPoints);
        }
        return isLoading;
    }


    public double render(Mesh mesh, String string, double x, double y, Color color, double scale) {
        if (tryLoadString(string))return x;

        y += ascent * this.scale * scale;
        for (int i = 0; i < string.length(); i++) {
            int cp = string.charAt(i);
            CharData c = charMap.get(cp);
            if (c == null) {
                continue;
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

    private record CharData(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1,
                            float xAdvance) {
    }
}
