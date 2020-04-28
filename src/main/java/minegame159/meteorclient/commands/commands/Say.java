package minegame159.meteorclient.commands.commands;

//Created by squidoodly 18/04/2020

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;


public class Say extends Command {

    public Say(){
        super("say", "Sends messages in chat.");
    }

    public void run(String[] args) {
        if (args.length != 0) {
            String message = args[0];
            for (int i = 1; i < args.length; i++) {
                message = message + " " + args[i];
            }
            MinecraftClient.getInstance().getNetworkHandler().sendPacket(new ChatMessageC2SPacket(message));
        }else{
            Utils.sendMessage("#redInvalid string length.");
        }
    }
}
