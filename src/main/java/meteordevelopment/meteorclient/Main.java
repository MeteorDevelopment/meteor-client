/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient;

import net.minecraft.util.Util;

import javax.swing.*;
import java.io.File;

public class Main {
    public static void main(String[] args) throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        int option = JOptionPane.showOptionDialog(
                null,
                "To install Meteor Client you need to put it in your mods folder and run Fabric for latest Minecraft version.",
                "Meteor Client",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                new String[] { "Open Wiki", "Open Mods Folder" },
                null
        );

        switch (option) {
            case 0 -> Util.getOperatingSystem().open("https://github.com/MeteorDevelopment/meteor-client/wiki/Installation");
            case 1 -> {
                String path = switch (Util.getOperatingSystem()) {
                    case WINDOWS -> System.getenv("AppData") + "/.minecraft/mods";
                    case OSX -> System.getProperty("user.home") + "/Library/Application Support/minecraft/mods";
                    default -> System.getProperty("user.home") + "/.minecraft";
                };

                File mods = new File(path);
                if (!mods.exists()) mods.mkdirs();

                Util.getOperatingSystem().open(mods);
            }
        }
    }
}
