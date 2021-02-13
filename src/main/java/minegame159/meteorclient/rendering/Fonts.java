/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.rendering;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.rendering.text.CustomTextRenderer;

import java.io.*;

public class Fonts {
    public static void reset() {
        File[] files = MeteorClient.FOLDER.exists() ? MeteorClient.FOLDER.listFiles() : new File[0];
        if (files == null) {
            return;
        }
        for (File file : files) {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".ttf")) {
                file.delete();
            }
        }
    }

    public static void init() {
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
                if (!fontFile.getParentFile().mkdirs())
                    throw new IOException("meteor-client directory could not be created");

                InputStream in = MeteorClient.class.getResourceAsStream("/assets/meteor-client/JetBrainsMono-Regular.ttf");
                if (in == null) throw new IOException("JetbrainsMono-Regular.ttf could not be found");
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

        MeteorClient.FONT = new CustomTextRenderer(fontFile);
    }
}
