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
                "To install Meteor Client you need to put it in your mods folder along with Fabric API. Then run Fabric for latest minecraft version.",
                "Meteor Client",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                new String[] { "Open Fabric link", "Open Fabric API link", "Open mods folder" },
                null
        );

        if (option == 0) {
            openUrl("https://fabricmc.net");
        } else if (option == 1) {
            openUrl("https://www.curseforge.com/minecraft/mc-mods/fabric-api");
        } else if (option == 2) {
            String os = System.getProperty("os.name").toLowerCase();

            try {
                if (os.contains("win")) {
                    if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(new File(System.getenv("AppData") + "/.minecraft/mods"));
                    }
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec("open " + System.getProperty("user.home") + "/Library/Application Support/minecraft");
                } else if (os.contains("nix") || os.contains("nux")) {
                    Runtime.getRuntime().exec("xdg-open " + System.getProperty("user.home") + "/.minecraft");
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
