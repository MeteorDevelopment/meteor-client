package minegame159.meteorclient.events.packets;

import minegame159.jes.Event;
import net.minecraft.client.network.packet.PlaySoundS2CPacket;

public class PlaySoundPacketEvent extends Event {
    public PlaySoundS2CPacket packet;

    @Override
    public boolean isCancellable() {
        return false;
    }
}
