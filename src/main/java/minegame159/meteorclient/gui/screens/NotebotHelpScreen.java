package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.WindowScreen;
import minegame159.meteorclient.gui.widgets.containers.WTable;

public class NotebotHelpScreen extends WindowScreen {


    public NotebotHelpScreen(GuiTheme theme) {
        super(theme, "Notebot - Help");

        WTable list = add(theme.table()).expandCellX().widget();

        list.add(theme.label("Loading songs", true));
        list.row();
        list.add(theme.label("To load songs you need to put file with supported format inside: "));
        list.row();
        list.add(theme.label(String.format(" \"%s\"", MeteorClient.FOLDER.toPath().resolve("notebot").toString())));
        list.row();

        list.add(theme.label("Supported formats", true)).padTop(10);
        list.row();
        list.add(theme.label("- Classic .nbs files"));
        list.row();
        list.add(theme.label("- OpenNBS v5 .nbs files"));
        list.row();
        list.add(theme.label("- .txt files using format <tick>:<note>"));
        list.row();

        list.add(theme.label("Playing", true)).padTop(10);
        list.row();
        list.add(theme.label("To play a song you can either:"));
        list.row();
        list.add(theme.label("-  place noteblocks around you in a 5 block radius"));
        list.row();
        list.add(theme.label("-  hold noteblocks in your hotbar and let the module do all the work"));
        list.row();
        list.add(theme.label("To start playing a song you can Press the \"Load\" button next to the song you want to load or use the .notebot command"));
        list.row();

        list.add(theme.label("Recording", true)).padTop(10);
        list.row();
        list.add(theme.label("You can also record in-game sound to play them back later"));
        list.row();
        list.add(theme.label("1. Run \".notebot record start\" to start recording"));
        list.row();
        list.add(theme.label("2. Stand next to some noteblocks"));
        list.row();
        list.add(theme.label("3. Run \".notebot record save <name>\""));
        list.row();
        list.add(theme.label("After that you will see your new recording inside the list of recordings"));
        list.row();
    }
    

}
