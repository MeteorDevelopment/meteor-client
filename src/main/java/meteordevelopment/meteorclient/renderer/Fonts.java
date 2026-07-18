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
import java.util.Locale;
import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Fonts {
    public static final String[] BUILTIN_FONTS = {"JetBrains Mono", "Comfortaa", "Tw Cen MT", "Pixelation"};
    private static final List<String> FALLBACK_FONT_FAMILIES = List.of(
        "Noto Sans CJK SC",
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
    );

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
        CustomTextRenderer previous = RENDERER;
        if (previous != null && previous.fontFace.equals(fontFace)) return;

        CustomTextRenderer replacement;
        try {
            replacement = new CustomTextRenderer(fontFace);
        } catch (Exception e) {
            if (fontFace.equals(DEFAULT_FONT)) {
                throw new RuntimeException("Failed to load default font: " + fontFace, e);
            }

            MeteorClient.LOG.error("Failed to load font: {}", fontFace, e);
            load(Fonts.DEFAULT_FONT);
            return;
        }

        RENDERER = replacement;
        if (previous != null) previous.destroy();
        MeteorClient.EVENT_BUS.post(CustomFontChangedEvent.get());

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

    public static Optional<FontFace> getFallbackFont(FontFace primary) {
        for (String familyName : FALLBACK_FONT_FAMILIES) {
            FontFace font = findFallbackFont(familyName, primary);
            if (font != null) return Optional.of(font);
        }

        return Optional.empty();
    }

    public static List<ByteBuffer> readFontBuffers(FontFace primary) throws IOException {
        List<ByteBuffer> buffers = new ArrayList<>();
        buffers.add(primary.readToDirectByteBuffer());

        getFallbackFont(primary)
            .flatMap(Fonts::readFallbackFont)
            .ifPresent(buffers::add);

        return List.copyOf(buffers);
    }

    private static Optional<ByteBuffer> readFallbackFont(FontFace font) {
        try {
            return Optional.of(font.readToDirectByteBuffer());
        } catch (IOException e) {
            MeteorClient.LOG.warn("Failed to load fallback font: {}", font, e);
            return Optional.empty();
        }
    }

    private static FontFace findFallbackFont(String familyName, FontFace primary) {
        FontFamily family = getFamily(familyName);

        if (family == null) {
            String needle = familyName.toLowerCase(Locale.ROOT);

            for (FontFamily fontFamily : FONT_FAMILIES) {
                if (fontFamily.getName().toLowerCase(Locale.ROOT).contains(needle)) {
                    family = fontFamily;
                    break;
                }
            }
        }

        if (family == null) return null;
        if (family.getName().equalsIgnoreCase(primary.info.family())) return null;

        FontFace styleMatch = family.get(primary.info.type());
        if (styleMatch != null) return styleMatch;

        FontFace regular = family.get(FontInfo.Type.Regular);
        if (regular != null) return regular;

        for (FontInfo.Type type : FontInfo.Type.values()) {
            if (type == primary.info.type() || type == FontInfo.Type.Regular) continue;

            FontFace font = family.get(type);
            if (font != null) return font;
        }

        return null;
    }
}
