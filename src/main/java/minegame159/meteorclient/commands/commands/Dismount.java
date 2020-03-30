package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;

public class Dismount extends Command {
    public Dismount() {
        super("dismount", "Dismounts you from entity you are riding.");
    }

    @Override
    public void run(String[] args) {
        //if (MC.player.hasVehicle()) {
            MC.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(0, 0, false, true));
        //}
    }
}
