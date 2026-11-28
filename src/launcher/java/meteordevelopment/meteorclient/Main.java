/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

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
            new String[]{"Open Wiki", "Open Mods Folder"},
            null
        );

        switch (option) {
            case 0:
                getOS().open("https://meteorclient.com/faq/installation");
                break;
            case 1: {
                File mods = new File(getModsFolder());
                if (!mods.exists()) mods.mkdirs();
                getOS().open(mods);
                break;
            }
        }
    }

    private static String getModsFolder() {
        String userHome = System.getProperty("user.home");
        switch (getOS()) {
            case WINDOWS:
                return System.getenv("AppData") + "/.minecraft/mods";
            case OSX:
                return userHome + "/Library/Application Support/minecraft/mods";
            default:
                return userHome + "/.minecraft/mods";
        }
    }

    private static OperatingSystem getOS() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);

        if (os.contains("linux") || os.contains("unix")) return OperatingSystem.LINUX;
        if (os.contains("mac")) return OperatingSystem.OSX;
        if (os.contains("win")) return OperatingSystem.WINDOWS;

        return OperatingSystem.UNKNOWN;
    }

    private enum OperatingSystem {
        LINUX,
        WINDOWS {
            @Override
            protected String[] getURLOpenCommand(URL url) {
                return new String[]{"rundll32", "url.dll,FileProtocolHandler", url.toString()};
            }
        },
        OSX {
            @Override
            protected String[] getURLOpenCommand(URL url) {
                return new String[]{"open", url.toString()};
            }
        },
        UNKNOWN;

        public void open(URL url) {
            try {
                Runtime.getRuntime().exec(getURLOpenCommand(url));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void open(String url) {
            try {
                open(new URI(url).toURL());
            } catch (URISyntaxException | MalformedURLException e) {
                e.printStackTrace();
            }
        }

        public void open(File file) {
            try {
                open(file.toURI().toURL());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        protected String[] getURLOpenCommand(URL url) {
            String string = url.toString();

            if ("file".equals(url.getProtocol())) {
                string = string.replace("file:", "file://");
            }

            return new String[]{"xdg-open", string};
        }
    }
}
