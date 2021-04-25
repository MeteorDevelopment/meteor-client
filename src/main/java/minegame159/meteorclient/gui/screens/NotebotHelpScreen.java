package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.WindowScreen;

public class NotebotHelpScreen extends WindowScreen {


    public NotebotHelpScreen(GuiTheme theme) {
        super(theme, "Notebot - Help");

        add(theme.label("Loading songs", true)).widget();
        add(theme.label("To load songs you need to put a file with supported format inside: "));
        add(theme.label(String.format(" \"%s\"", MeteorClient.FOLDER.toPath().resolve("notebot").toString())));

        add(theme.label("Supported formats", true)).padTop(10);
        add(theme.label("- Classic .nbs files"));
        add(theme.label("- OpenNBS v5 .nbs files"));
        add(theme.label("- .txt files using format <tick>:<note>"));

        add(theme.label("Playing", true)).padTop(10);
        add(theme.label("To play a song you can either:"));
        add(theme.label("-  place noteblocks around you in a 5 block radius"));
        add(theme.label("-  hold noteblocks in your hotbar and let the module do all the work"));
        add(theme.label("To start playing a song you can Press the \"Load\" button next to the song you want to load or use the .notebot command"));

        add(theme.label("Recording", true)).padTop(10);
        add(theme.label("You can also record in-game sound to play them back later"));
        add(theme.label("1. Run \".notebot record start\" to start recording"));
        add(theme.label("2. Stand next to some noteblocks"));
        add(theme.label("3. Run \".notebot record save <name>\""));
        add(theme.label("After that you will see your new recording inside the list of recordings"));
    }
    

}
