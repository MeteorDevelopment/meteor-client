/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Locale;

public class Main {
    public static void main(String[] args) throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        int option = JOptionPane.showOptionDialog(
                null,
                "To install Meteor Client you need to put it in your mods folder and run Fabric for latest Minecraft version.",
                "Meteor Client",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                new String[] { "Open Wiki", "Open Mods Folder", "Join our Discord" },
                null
        );

        switch (option) {
            case 0: {
                try {
                    Desktop.getDesktop().browse(URI.create("https://meteorclient.com/faq/installation"));
                } catch (IOException ignored) {}
                break;
            }
            case 1: {
                String path;

                switch (getOS()) {
                    case WINDOWS: path = System.getenv("AppData") + "/.minecraft/mods"; break;
                    case OSX:     path = System.getProperty("user.home") + "/Library/Application Support/minecraft/mods"; break;
                    default:      path = System.getProperty("user.home") + "/.minecraft/mods"; break;
                }

                File mods = new File(path);
                if (!mods.exists()) mods.mkdirs();
                try {
                    Desktop.getDesktop().open(mods);
                } catch (IOException ignored) {}
                break;
            }
            case 2: {
                try {
                    Desktop.getDesktop().browse(URI.create("https://discord.com/invite/bBGQZvd"));
                } catch (IOException ignored) {}
                break;
            }
        }
    }

    private static OperatingSystem getOS() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        if (os.contains("linux") || os.contains("unix"))  return OperatingSystem.LINUX;
        if (os.contains("mac")) return OperatingSystem.OSX;
        if (os.contains("win")) return OperatingSystem.WINDOWS;

        return OperatingSystem.UNKNOWN;
    }

    private enum OperatingSystem {
        LINUX,
        WINDOWS,
        OSX,
        UNKNOWN;
    }
}
