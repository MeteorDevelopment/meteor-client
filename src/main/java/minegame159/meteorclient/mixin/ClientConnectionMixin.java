package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.packets.ReceivePacketEvent;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "disconnect", at = @At("HEAD"))
    private void onDisconnect(Text disconnectReason, CallbackInfo info) {
        if (Utils.canUpdate() && !MeteorClient.IS_DISCONNECTING) {
            MeteorClient.IS_DISCONNECTING = true;
            MeteorClient.EVENT_BUS.post(EventStore.gameDisconnectedEvent(disconnectReason));
        }
    }

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo info) {
        ReceivePacketEvent event = EventStore.receivePacketEvent(packet);
        MeteorClient.EVENT_BUS.post(event);

        if (event.isCancelled()) info.cancel();
    }
}
