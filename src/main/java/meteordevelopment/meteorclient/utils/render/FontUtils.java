/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.*;
import meteordevelopment.meteorclient.utils.Utils;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FontUtils {

    public static FontInfo getSysFontInfo(File file) {
        return getFontInfo(stream(file));
    }

    public static FontInfo getBuiltinFontInfo(String builtin) {
        return getFontInfo(stream(builtin));
    }

    public static FontInfo getFontInfo(InputStream stream) {
        if (stream == null) return null;

        byte[] bytes = Utils.readBytes(stream);
        if (bytes.length < 5) return null;

        if (
            bytes[0] != 0 ||
            bytes[1] != 1 ||
            bytes[2] != 0 ||
            bytes[3] != 0 ||
            bytes[4] != 0
        ) return null;

        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length).put(bytes).flip();
        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, buffer)) return null;

        ByteBuffer nameBuffer = STBTruetype.stbtt_GetFontNameString(fontInfo, STBTruetype.STBTT_PLATFORM_ID_MICROSOFT, STBTruetype.STBTT_MS_EID_UNICODE_BMP, STBTruetype.STBTT_MS_LANG_ENGLISH, 1);
        ByteBuffer typeBuffer = STBTruetype.stbtt_GetFontNameString(fontInfo, STBTruetype.STBTT_PLATFORM_ID_MICROSOFT, STBTruetype.STBTT_MS_EID_UNICODE_BMP, STBTruetype.STBTT_MS_LANG_ENGLISH, 2);
        if (typeBuffer == null || nameBuffer == null) return null;

        return new FontInfo(
            StandardCharsets.UTF_16.decode(nameBuffer).toString(),
            FontInfo.Type.fromString(StandardCharsets.UTF_16.decode(typeBuffer).toString())
        );
    }

    public static void loadBuiltin(List<FontFamily> fontList, String builtin) {
        FontInfo fontInfo = FontUtils.getBuiltinFontInfo(builtin);
        if (fontInfo == null) return;

        FontFace fontFace = new BuiltinFontFace(fontInfo, builtin);
        if (!addFont(fontList, fontFace)) {
            MeteorClient.LOG.warn("Failed to load builtin font {}", fontFace);
        }
    }

    public static void loadExternal(List<FontFamily> fontList, File dir) {
        if (dir.mkdir() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((file) -> (file.isFile() && file.getName().endsWith(".ttf") || file.isDirectory()));
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadExternal(fontList, file);
                continue;
            }

            FontInfo fontInfo = FontUtils.getSysFontInfo(file);
            if (fontInfo == null) continue;

            boolean isBuiltin = false;
            for (String builtinFont : Fonts.BUILTIN_FONTS) {
                if (builtinFont.equals(fontInfo.family())) {
                    isBuiltin = true;
                    break;
                }
            }
            if (isBuiltin) continue;

            FontFace fontFace = new ExternalFontFace(fontInfo, file.toPath());
            if (!addFont(fontList, fontFace)) {
                MeteorClient.LOG.warn("Failed to load external font {}", fontFace);
            }
        }
    }

    public static boolean addFont(List<FontFamily> fontList, FontFace font) {
        if (font == null) return false;

        FontInfo info = font.info;

        FontFamily family = Fonts.getFamily(info.family());
        if (family == null) {
            family = new FontFamily(info.family());
            fontList.add(family);
        }

        if (family.hasType(info.type())) return false;

        return family.addFont(font);
    }

    public static InputStream stream(String builtin) {
        return FontUtils.class.getResourceAsStream("/assets/" + MeteorClient.MOD_ID + "/fonts/" + builtin + ".ttf");
    }

    public static InputStream stream(File file) {
        try {
            return new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
