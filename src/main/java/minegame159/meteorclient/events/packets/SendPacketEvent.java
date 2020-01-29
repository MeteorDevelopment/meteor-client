package minegame159.meteorclient.events.packets;

import minegame159.meteorclient.events.Cancellable;
import net.minecraft.network.Packet;

public class SendPacketEvent extends Cancellable {
    public Packet<?> packet;
}
