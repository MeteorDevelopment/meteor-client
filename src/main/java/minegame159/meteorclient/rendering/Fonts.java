/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.rendering;

import minegame159.meteorclient.MeteorClient;
import net.minecraft.util.Pair;

import java.awt.*;
import java.io.*;

public class Fonts {
    private static MyFont[] fonts;
    private static MyFont title;

    public static void reset() {
        File[] files = MeteorClient.FOLDER.exists() ? MeteorClient.FOLDER.listFiles() : new File[0];
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".ttf") || file.getName().endsWith(".TTF")) {
                    file.delete();
                }
            }
        }
    }

    public static void init() {
        fonts = new MyFont[5];

        File[] files = MeteorClient.FOLDER.exists() ? MeteorClient.FOLDER.listFiles() : new File[0];
        File fontFile = null;
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".ttf") || file.getName().endsWith(".TTF")) {
                    fontFile = file;
                    break;
                }
            }
        }

        if (fontFile == null) {
            try {
                fontFile = new File(MeteorClient.FOLDER, "JetBrainsMono-Regular.ttf");
                fontFile.getParentFile().mkdirs();

                InputStream in = MeteorClient.class.getResourceAsStream("/assets/meteor-client/JetBrainsMono-Regular.ttf");
                OutputStream out = new FileOutputStream(fontFile);

                byte[] bytes = new byte[255];
                int read;
                while ((read = in.read(bytes)) > 0) out.write(bytes, 0, read);

                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            MeteorClient.FONT_2X = new MFont(Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(16f * 2), true, true);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < 5; i++) {
            fonts[i] = new MyFont(fontFile, (int) Math.round(18 * ((i * 0.5) + 1)));
        }
    }

    public static MyFont get(int i) {
        return fonts[i];
    }

    public static MyFont get() {
        return fonts[0];
    }

    public static Pair<MyFont, Double> get(double scale) {
        double scaleA = Math.floor(scale * 10) / 10;

        int scaleI;
        if (scaleA >= 3) scaleI = 5;
        else if (scaleA >= 2.5) scaleI = 4;
        else if (scaleA >= 2) scaleI = 3;
        else if (scaleA >= 1.5) scaleI = 2;
        else scaleI = 1;

        return new Pair<>(Fonts.get(scaleI - 1), (scale - (((scaleI - 1) * 0.5) + 1)) / scaleA + 1);
    }
}
