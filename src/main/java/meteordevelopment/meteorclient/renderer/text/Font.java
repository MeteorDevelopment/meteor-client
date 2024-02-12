/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.renderer.Mesh;
import meteordevelopment.meteorclient.utils.render.ByteTexture;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.texture.AbstractTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedList;

public class Font {
    public AbstractTexture texture;
    private final int height;
    private float scale;
    private float ascent;
    private final Int2ObjectOpenHashMap<CharData> charMap = new Int2ObjectOpenHashMap<>();
    private final static int size = 2048;
    //字体缩放基于图像....
    //只能动态加载中文字体了吗
    //完成了
    ByteBuffer buffer;

    public class rendChar {
        public rendChar(int start, int size) {
            this.start = start;
            this.size = size;
        }

        public final int start;
        public final int size;
    }

    LinkedList<rendChar> render = new LinkedList<>();

    public Font(ByteBuffer buffer, int height) {
        this.height = height;
        this.buffer = buffer;

        // Initialize font
        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        STBTruetype.stbtt_InitFont(fontInfo, buffer);

        // Allocate buffers
        ByteBuffer bitmap = BufferUtils.createByteBuffer(size * size);
        ArrayList<STBTTPackedchar.Buffer> cdata = new ArrayList<>();
        render.add(new rendChar(32, 95));
        render.add(new rendChar(160, 96));
        render.add(new rendChar(256, 128));
        render.add(new rendChar(880, 144));
        render.add(new rendChar(1024, 256));
        render.add(new rendChar(8734, 1));
        for (rendChar rendChar : render) {
            cdata.add(STBTTPackedchar.create(rendChar.size));
        }


        // create and initialise packing context
        STBTTPackContext packContext = STBTTPackContext.create();
        STBTruetype.stbtt_PackBegin(packContext, bitmap, size, size, 0, 1);

        // create the pack range, populate with the specific packing ranges
        STBTTPackRange.Buffer packRange = STBTTPackRange.create(cdata.size());
        for (int i = 0; i < render.size(); i++) {
            rendChar rendChar = render.get(i);
            packRange.put(STBTTPackRange.create().set(height, rendChar.start, null, rendChar.size, cdata.get(i), (byte) 2, (byte) 2));

        }


        packRange.flip();

        // write and finish
        STBTruetype.stbtt_PackFontRanges(packContext, buffer, 0, packRange);
        STBTruetype.stbtt_PackEnd(packContext);

        // Create texture object and get font scale
        texture = new ByteTexture(size, size, bitmap, ByteTexture.Format.A, ByteTexture.Filter.Linear, ByteTexture.Filter.Linear);
        scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, height);

        // Get font vertical ascent
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
            this.ascent = ascent.get(0);
        }

        for (int i = 0; i < cdata.size(); i++) {
            STBTTPackedchar.Buffer cbuf = cdata.get(i);
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
            }
        }
    }

    public double getWidth(String string, int length) {
        double width = 0;

        for (int i = 0; i < length; i++) {
            int cp = string.charAt(i);
            CharData c = charMap.get(cp);
            if (c == null) c = charMap.get(32);

            width += c.xAdvance;
        }

        return width;
    }

    public int getHeight() {
        return height;
    }

    boolean isLock = false;
    public void addChar(char c) {
        render.add(new rendChar(c, 1));
        if (isLock)return;
        isLock = true;
        Thread thread = new Thread(() -> {
            // Initialize font
            STBTTFontinfo fontInfo = STBTTFontinfo.create();
            STBTruetype.stbtt_InitFont(fontInfo, buffer);

            // Allocate buffers
            ByteBuffer bitmap = BufferUtils.createByteBuffer(size * size);
            ArrayList<STBTTPackedchar.Buffer> cdata = new ArrayList<>();


            for (rendChar rendChar : render) {
                cdata.add(STBTTPackedchar.create(rendChar.size));
            }


            // create and initialise packing context
            STBTTPackContext packContext = STBTTPackContext.create();
            STBTruetype.stbtt_PackBegin(packContext, bitmap, size, size, 0, 1);

            // create the pack range, populate with the specific packing ranges
            STBTTPackRange.Buffer packRange = STBTTPackRange.create(cdata.size());
            for (int i = 0; i < render.size(); i++) {
                rendChar rendChar = render.get(i);
                packRange.put(STBTTPackRange.create().set(height, rendChar.start, null, rendChar.size, cdata.get(i), (byte) 2, (byte) 2));

            }


            packRange.flip();

            // write and finish
            STBTruetype.stbtt_PackFontRanges(packContext, buffer, 0, packRange);
            STBTruetype.stbtt_PackEnd(packContext);

            // Create texture object and get font scale
            texture = new ByteTexture(size, size, bitmap, ByteTexture.Format.A, ByteTexture.Filter.Linear, ByteTexture.Filter.Linear);
            scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, height);

            // Get font vertical ascent
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer ascent = stack.mallocInt(1);
                STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
                this.ascent = ascent.get(0);
            } finally {
                isLock = false;
            }

            for (int i = 0; i < cdata.size(); i++) {
                STBTTPackedchar.Buffer cbuf = cdata.get(i);
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
                }
            }
            isLock = false;
        });
        thread.setPriority(5);

        thread.start();
    }

//    public void addChar(char[] ch){
//        STBTTPackedchar.Buffer packedChars = STBTTPackedchar.create(ch.length);
//        STBTTPackRange.Buffer packRange = STBTTPackRange.create(1);
//        packRange.put(STBTTPackRange.create().set(height, 8734, null, 1, packedChars, (byte) 2, (byte) 2));
//
//
//        int offset = packRange.get(0).first_unicode_codepoint_in_range();
//
//        for (int j = 0; j < packedChars.capacity(); j++) {
//            STBTTPackedchar packedChar = packedChars.get(j);
//
//            float ipw = 1f / size; // pixel width and height
//            float iph = 1f / size;
//
//            charMap.put(j + offset, new CharData(
//                packedChar.xoff(),
//                packedChar.yoff(),
//                packedChar.xoff2(),
//                packedChar.yoff2(),
//                packedChar.x0() * ipw,
//                packedChar.y0() * iph,
//                packedChar.x1() * ipw,
//                packedChar.y1() * iph,
//                packedChar.xadvance()
//            ));
//        }
//    }

    public double render(Mesh mesh, String string, double x, double y, Color color, double scale) {
        y += ascent * this.scale * scale;

        for (int i = 0; i < string.length(); i++) {
            int cp = string.charAt(i);
            CharData c = charMap.get(cp);
            if (c == null) {
                if (string.charAt(i) >= 10000)
                    addChar(string.charAt(i));
                if (c == null)
                    c = charMap.get(32);
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
