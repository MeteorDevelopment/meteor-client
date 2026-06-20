/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.CustomFontChangedEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.render.FontUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Fonts {
    public static final String[] BUILTIN_FONTS = {"JetBrains Mono", "Comfortaa", "Tw Cen MT", "Pixelation"};
    private static final String[] FALLBACK_FONT_FAMILIES = {
        "Noto Sans SC",
        "Microsoft YaHei",
        "Microsoft YaHei UI",
        "DengXian",
        "SimHei",
        "SimSun",
        "KaiTi",
        "Microsoft JhengHei",
        "Malgun Gothic",
        "Yu Gothic",
        "MS Gothic",
        "Source Han Sans SC",
        "WenQuanYi Zen Hei",
        "PingFang SC",
        "Hiragino Sans GB"
    };

    public static String DEFAULT_FONT_FAMILY;
    public static FontFace DEFAULT_FONT;

    public static final List<FontFamily> FONT_FAMILIES = new ArrayList<>();
    public static CustomTextRenderer RENDERER;

    private Fonts() {
    }

    @PreInit
    public static void refresh() {
        FONT_FAMILIES.clear();

        for (String builtinFont : BUILTIN_FONTS) {
            FontUtils.loadBuiltin(FONT_FAMILIES, builtinFont);
        }

        for (String fontPath : FontUtils.getSearchPaths()) {
            FontUtils.loadSystem(FONT_FAMILIES, new File(fontPath));
        }

        FONT_FAMILIES.sort(Comparator.comparing(FontFamily::getName));

        MeteorClient.LOG.info("Found {} font families.", FONT_FAMILIES.size());

        DEFAULT_FONT_FAMILY = FontUtils.getBuiltinFontInfo(BUILTIN_FONTS[1]).family();
        DEFAULT_FONT = getFamily(DEFAULT_FONT_FAMILY).get(FontInfo.Type.Regular);

        Config config = Config.get();
        load(config != null ? config.font.get() : DEFAULT_FONT);
    }

    public static void load(FontFace fontFace) {
        if (RENDERER != null) {
            if (RENDERER.fontFace.equals(fontFace)) return;
            else RENDERER.destroy();
        }

        try {
            RENDERER = new CustomTextRenderer(fontFace);
            MeteorClient.EVENT_BUS.post(CustomFontChangedEvent.get());
        } catch (Exception e) {
            if (fontFace.equals(DEFAULT_FONT)) {
                throw new RuntimeException("Failed to load default font: " + fontFace, e);
            }

            MeteorClient.LOG.error("Failed to load font: {}", fontFace, e);
            load(Fonts.DEFAULT_FONT);
        }

        if (mc.screen instanceof WidgetScreen widgetScreen && Config.get().customFont.get()) {
            widgetScreen.invalidate();
        }
    }

    public static FontFamily getFamily(String name) {
        for (FontFamily fontFamily : Fonts.FONT_FAMILIES) {
            if (fontFamily.getName().equalsIgnoreCase(name)) {
                return fontFamily;
            }
        }

        return null;
    }

    public static List<FontFace> getFallbackFonts(FontFace primary) {
        List<FontFace> fonts = new ArrayList<>();

        for (String familyName : FALLBACK_FONT_FAMILIES) {
            FontFace font = getFallbackFont(familyName, primary);
            if (font != null) {
                fonts.add(font);
                break;
            }
        }

        return fonts;
    }

    public static List<ByteBuffer> readFontBuffers(FontFace primary) throws IOException {
        List<ByteBuffer> buffers = new ArrayList<>();
        buffers.add(primary.readToDirectByteBuffer());

        for (FontFace fallbackFont : getFallbackFonts(primary)) {
            try {
                buffers.add(fallbackFont.readToDirectByteBuffer());
            } catch (IOException e) {
                MeteorClient.LOG.warn("Failed to load fallback font: {}", fallbackFont, e);
            }
        }

        return buffers;
    }

    private static FontFace getFallbackFont(String familyName, FontFace primary) {
        FontFamily family = getFamily(familyName);

        if (family == null) {
            String needle = familyName.toLowerCase();

            for (FontFamily fontFamily : FONT_FAMILIES) {
                if (fontFamily.getName().toLowerCase().contains(needle)) {
                    family = fontFamily;
                    break;
                }
            }
        }

        if (family == null) return null;

        for (FontInfo.Type type : FontInfo.Type.values()) {
            FontFace font = family.get(type);
            if (font != null && !font.info.equals(primary.info)) return font;
        }

        return null;
    }
}
