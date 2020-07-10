package minegame159.meteorclient.commands.commands;

//Created by squidoodly 01/07/2020

import minegame159.meteorclient.commands.Command;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class Ignore extends Command {
    public Ignore(){super("ignore", "Lets you ignore messages from specific players.");}

    private MinecraftClient mc = MinecraftClient.getInstance();

    public static List<String> ignoredPlayers = new ArrayList<>();

    @Override
    public void run(String[] args) {
        ignoredPlayers.add(args[0]);
    }
}
