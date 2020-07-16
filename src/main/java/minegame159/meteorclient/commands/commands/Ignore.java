package minegame159.meteorclient.commands.commands;

//Created by squidoodly 01/07/2020

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Ignore extends Command {
    public Ignore(){super("ignore", "Lets you ignore messages from specific players.");}

    public static List<String> ignoredPlayers = new ArrayList<>();

    @Override
    public void run(String[] args) {
        if (args.length == 0) {
            Utils.sendMessage("#blue[Meteor]: #whiteIgnoring #gray%d #white people:", ignoredPlayers.size());
            for (String player : ignoredPlayers) {
                Utils.sendMessage("#blue[Meteor]: #gray- #white%s", player);
            }
        } else {
            if (ignoredPlayers.remove(args[0])) {
                Utils.sendMessage("#blue[Meteor]: #whiteRemoved #gray%s #whitefrom list of ignored people.", args[0]);
            } else {
                ignoredPlayers.add(args[0]);
                Utils.sendMessage("#blue[Meteor]: #whiteAdded #gray%s #whiteto list of ignored people.", args[0]);
            }
        }
    }

    public static void load() {
        File file = new File(MeteorClient.FOLDER, "ignored_players.txt");
        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));

                String line;
                while ((line = reader.readLine()) != null) Ignore.ignoredPlayers.add(line.split(" ")[0]);

                reader.close();
            } catch (IOException ignored) {
                Ignore.ignoredPlayers = new ArrayList<>();
            }
        }
    }

    public static void save() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(MeteorClient.FOLDER, "ignored_players.txt")));
            for (String name: Ignore.ignoredPlayers) {
                writer.write(name);
                writer.write(" OwO\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
