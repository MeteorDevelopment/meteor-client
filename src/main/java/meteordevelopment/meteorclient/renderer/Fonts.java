/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.CustomFontChangedEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Init;
import meteordevelopment.meteorclient.utils.InitStage;
import meteordevelopment.meteorclient.utils.render.FontUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.render.FontUtils.copyBuiltin;

public class Fonts {
    private static final String[] BUILTIN_FONTS = { "JetBrains Mono", "Comfortaa", "Tw Cen MT", "Pixelation" };

    public static String DEFAULT_FONT_FAMILY;
    public static FontFace DEFAULT_FONT;

    public static final List<FontFamily> FONT_FAMILIES = new ArrayList<>();
    public static CustomTextRenderer RENDERER;

    @Init(stage = InitStage.Pre, dependencies = Shaders.class)
    public static void refresh() {
        File target = FontUtils.getDir(FontUtils.getUFontDirs());
        for (String builtinFont : BUILTIN_FONTS) {
            copyBuiltin(builtinFont, target);
        }

        FONT_FAMILIES.clear();

        for (String fontPath : FontUtils.getSearchPaths()) {
            FontUtils.collectFonts(FONT_FAMILIES, new File(fontPath), file -> {
                if (file.getAbsolutePath().endsWith(BUILTIN_FONTS[1] + ".ttf")) {
                    DEFAULT_FONT_FAMILY = FontUtils.getFontInfo(file).family();
                }
            });
        }

        FONT_FAMILIES.sort(Comparator.comparing(FontFamily::getName));

        MeteorClient.LOG.info("Found {} font families.", FONT_FAMILIES.size());

        DEFAULT_FONT = getFamily(DEFAULT_FONT_FAMILY).get(FontFace.Type.Regular);

        Config config = Config.get();
        load(config != null ? config.font.get() : DEFAULT_FONT);
    }

    public static void load(FontFace fontFace) {
        if (RENDERER != null && RENDERER.fontFace.equals(fontFace)) return;

        try {
            RENDERER = new CustomTextRenderer(fontFace);
            MeteorClient.EVENT_BUS.post(CustomFontChangedEvent.get());
        }
        catch (Exception e) {
            if (fontFace.equals(DEFAULT_FONT)) {
                throw new RuntimeException("Failed to load default font: " + fontFace, e);
            }

            MeteorClient.LOG.error("Failed to load font: " + fontFace, e);
            load(Fonts.DEFAULT_FONT);
        }

        if (mc.currentScreen instanceof WidgetScreen && Config.get().customFont.get()) {
            ((WidgetScreen) mc.currentScreen).invalidate();
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
}
