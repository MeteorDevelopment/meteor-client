package minegame159.meteorclient.events.packets;

import minegame159.meteorclient.events.Cancellable;
import net.minecraft.network.Packet;

public class ReceivePacketEvent extends Cancellable {
    public Packet<?> packet;
}
