/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Init;
import meteordevelopment.meteorclient.utils.InitStage;
import meteordevelopment.meteorclient.utils.files.StreamUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Fonts {
    private static final String[] BUILTIN_FONTS = { "JetBrains Mono.ttf", "Comfortaa.ttf", "Tw Cen MT.ttf", "Pixelation.ttf" };
    public static final String DEFAULT_FONT = "JetBrains Mono";
    private static final File FOLDER = new File(MeteorClient.FOLDER, "fonts");

    public static CustomTextRenderer CUSTOM_FONT;

    private static String lastFont = "";

    @Init(stage = InitStage.Pre)
    public static void init() {
        FOLDER.mkdirs();

        // Copy built in fonts if they not exist
        for (String font : BUILTIN_FONTS) {
            File file = new File(FOLDER, font);
            if (!file.exists()) {
                StreamUtils.copy(Fonts.class.getResourceAsStream("/assets/" + MeteorClient.MOD_ID + "/fonts/" + font), file);
            }
        }

        // Load default font
        CUSTOM_FONT = new CustomTextRenderer(new File(FOLDER, DEFAULT_FONT + ".ttf"));
        lastFont = DEFAULT_FONT;
    }

    @Init(stage = InitStage.Post)
    public static void load() {
        if (lastFont.equals(Config.get().font.get())) return;

        File file = new File(FOLDER, Config.get().font + ".ttf");
        if (!file.exists()) {
            Config.get().font.set(DEFAULT_FONT);
            file = new File(FOLDER, Config.get().font + ".ttf");
        }

        try {
            CUSTOM_FONT = new CustomTextRenderer(file);
        } catch (Exception ignored) {
            Config.get().font.set(DEFAULT_FONT);
            file = new File(FOLDER, Config.get().font + ".ttf");

            CUSTOM_FONT = new CustomTextRenderer(file);
        }

        if (mc.currentScreen instanceof WidgetScreen && Config.get().customFont.get()) {
            ((WidgetScreen) mc.currentScreen).invalidate();
        }

        lastFont = Config.get().font.get();
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
