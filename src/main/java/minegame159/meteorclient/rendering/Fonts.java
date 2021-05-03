/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.rendering;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.rendering.text.CustomTextRenderer;
import minegame159.meteorclient.systems.config.Config;
import minegame159.meteorclient.utils.files.StreamUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static minegame159.meteorclient.utils.Utils.mc;

public class Fonts {
    private static final String[] BUILTIN_FONTS = { "JetBrains Mono.ttf", "Comfortaa.ttf", "Tw Cen MT.ttf", "Pixelation.ttf" };
    public static final String DEFAULT_FONT = "JetBrains Mono";

    private static final File FOLDER = new File(MeteorClient.FOLDER, "fonts");

    private static String lastFont = "";
    private static CustomTextRenderer a;

    public static void init() {
        FOLDER.mkdirs();

        // Copy built in fonts if they not exist
        for (String font : BUILTIN_FONTS) {
            File file = new File(FOLDER, font);
            if (!file.exists()) {
                StreamUtils.copy(Fonts.class.getResourceAsStream("/assets/meteor-client/fonts/" + font), file);
            }
        }

        // Load default font
        MeteorClient.FONT = new CustomTextRenderer(new File(FOLDER, DEFAULT_FONT + ".ttf"));
        lastFont = DEFAULT_FONT;
    }

    public static void load() {
        if (lastFont.equals(Config.get().font)) return;

        File file = new File(FOLDER, Config.get().font + ".ttf");
        if (!file.exists()) {
            Config.get().font = DEFAULT_FONT;
            file = new File(FOLDER, Config.get().font + ".ttf");
        }

        try {
            MeteorClient.FONT = new CustomTextRenderer(file);
        } catch (Exception ignored) {
            Config.get().font = DEFAULT_FONT;
            file = new File(FOLDER, Config.get().font + ".ttf");

            MeteorClient.FONT = new CustomTextRenderer(file);
        }

        if (mc.currentScreen instanceof WidgetScreen && Config.get().customFont) {
            ((WidgetScreen) mc.currentScreen).invalidate();
        }

        lastFont = Config.get().font;
    }

    public static String[] getAvailableFonts() {
        List<String> fonts = new ArrayList<>(4);

        File[] files = FOLDER.listFiles(File::isFile);
        if (files != null) {
            for (File file : files) {
                int i = file.getName().lastIndexOf('.');
                if (file.getName().substring(i).equals(".ttf")) {
                    fonts.add(file.getName().substring(0, i));
                }
            }
        }

        return fonts.toArray(new String[0]);
    }
}
