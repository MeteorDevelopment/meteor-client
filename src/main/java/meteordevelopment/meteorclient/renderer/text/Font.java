/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import meteordevelopment.meteorclient.renderer.Mesh;
import meteordevelopment.meteorclient.utils.render.ByteTexture;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.texture.AbstractTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Font {
    public AbstractTexture texture;

    private final int height;
    private final float scale;
    private final float ascent;
    private final CharData[] charData;

    public Font(ByteBuffer buffer, int height) {
        this.height = height;

        // Initialize font
        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        STBTruetype.stbtt_InitFont(fontInfo, buffer);

        // Allocate STBTTPackedchar buffer
        charData = new CharData[128];
        STBTTPackedchar.Buffer cdata = STBTTPackedchar.create(charData.length);
        ByteBuffer bitmap = BufferUtils.createByteBuffer(2048 * 2048);

        // Create font texture
        STBTTPackContext packContext = STBTTPackContext.create();
        STBTruetype.stbtt_PackBegin(packContext, bitmap, 2048, 2048, 0, 1);
        STBTruetype.stbtt_PackSetOversampling(packContext, 2, 2);
        STBTruetype.stbtt_PackFontRange(packContext, buffer, 0, height, 32, cdata);
        STBTruetype.stbtt_PackEnd(packContext);

        // Create texture object and get font scale
        texture = new ByteTexture(2048, 2048, bitmap, ByteTexture.Format.A, ByteTexture.Filter.Linear, ByteTexture.Filter.Linear);
        scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, height);

        // Get font vertical ascent
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
            this.ascent = ascent.get(0);
        }

        // Populate charData array
        for (int i = 0; i < charData.length; i++) {
            STBTTPackedchar packedChar = cdata.get(i);

            float ipw = 1f / 2048;
            float iph = 1f / 2048;

            charData[i] = new CharData(
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
        }
    }

    public double getWidth(String string, int length) {
        double width = 0;

        for (int i = 0; i < length; i++) {
            int cp = string.charAt(i);
            if (cp < 32 || cp > 128) cp = 32;
            CharData c = charData[cp - 32];

            width += c.xAdvance;
        }

        return width;
    }

    public int getHeight() {
        return height;
    }

    public double render(Mesh mesh, String string, double x, double y, Color color, double scale) {
        y += ascent * this.scale * scale;

        for (int i = 0; i < string.length(); i++) {
            int cp = string.charAt(i);
            if (cp < 32 || cp > 128) cp = 32;
            CharData c = charData[cp - 32];

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
}
