/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        int option = JOptionPane.showOptionDialog(
                null,
                "To install Meteor Client you need to put it in your mods folder and run Fabric for latest Minecraft version.",
                "Meteor Client",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                new String[] { "Open Fabric link", "Open mods folder" },
                null
        );

        if (option == 0) {
            openUrl("http://fabricmc.net");
        } else if (option == 1) {
            String os = System.getProperty("os.name").toLowerCase();

            try {
                if (os.contains("win")) {
                    if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        String path = System.getenv("AppData") + "/.minecraft/mods";
                        new File(path).mkdirs();
                        Desktop.getDesktop().open(new File(path));
                    }
                } else if (os.contains("mac")) {
                    String path = System.getProperty("user.home") + "/Library/Application Support/minecraft/mods";
                    new File(path).mkdirs();
                    ProcessBuilder pb = new ProcessBuilder("open", path);
                    Process process = pb.start();
                } else if (os.contains("nix") || os.contains("nux")) {
                    String path = System.getProperty("user.home") + "/.minecraft";
                    new File(path).mkdirs();
                    Runtime.getRuntime().exec("xdg-open \"" + path + "\"");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void openUrl(String url) {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                }
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + url);
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec("xdg-open " + url);
            }
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }
}
