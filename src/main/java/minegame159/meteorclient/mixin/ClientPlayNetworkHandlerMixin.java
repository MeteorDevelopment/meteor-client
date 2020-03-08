package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(at = @At("TAIL"), method = "onGameJoin")
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo info) {
        MeteorClient.eventBus.post(EventStore.gameJoinedEvent());
    }

    @Inject(at = @At("HEAD"), method = "sendPacket", cancellable = true)
    public void onSendPacket(Packet packet, CallbackInfo info) {
        SendPacketEvent event = EventStore.sendPacketEvent(packet);
        MeteorClient.eventBus.post(event);
        if (event.isCancelled()) info.cancel();
    }

    @Inject(at = @At("HEAD"), method = "onPlaySound")
    private void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo info) {
        MeteorClient.eventBus.post(EventStore.playSoundPacketEvent(packet));
    }
}
