package minegame159.meteorclient.events.packets;

import minegame159.jes.Event;
import net.minecraft.network.Packet;

public class SendPacketEvent extends Event {
    public Packet packet;

    @Override
    public boolean isCancellable() {
        return true;
    }
}
