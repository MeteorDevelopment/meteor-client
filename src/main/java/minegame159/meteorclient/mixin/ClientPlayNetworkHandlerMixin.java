package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.packets.SendPacketEvent;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.movement.Velocity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Shadow private MinecraftClient client;

    @Inject(at = @At("TAIL"), method = "onGameJoin")
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo info) {
        MeteorClient.IS_DISCONNECTING = false;
        MeteorClient.EVENT_BUS.post(EventStore.gameJoinedEvent());
    }

    @Inject(at = @At("HEAD"), method = "sendPacket", cancellable = true)
    public void onSendPacket(Packet packet, CallbackInfo info) {
        SendPacketEvent event = EventStore.sendPacketEvent(packet);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) info.cancel();
    }

    @Inject(at = @At("HEAD"), method = "onPlaySound")
    private void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(EventStore.playSoundPacketEvent(packet));
    }

    @Inject(method = "onChunkData", at = @At("TAIL"))
    private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo info) {
        WorldChunk chunk = client.world.getChunk(packet.getX(), packet.getZ());
        MeteorClient.EVENT_BUS.post(EventStore.chunkDataEvent(chunk));
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("TAIL"))
    private void onContainerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(EventStore.containerSlotUpdateEvent(packet));
    }

    @Inject(method = "onEntitiesDestroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;removeEntity(I)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onEntityDestroy(EntitiesDestroyS2CPacket packet, CallbackInfo info, int i, int j) {
        MeteorClient.EVENT_BUS.post(EventStore.entityDestroyEvent(client.world.getEntityById(j)));
    }

    @Redirect(method = "onExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
    private void onExplosionVec3dAddProxy(ClientPlayerEntity player, Vec3d vec3d) {
        if (player != client.player) player.setVelocity(vec3d);

        double deltaX = vec3d.x - player.getVelocity().x;
        double deltaY = vec3d.y - player.getVelocity().y;
        double deltaZ = vec3d.z - player.getVelocity().z;

        Velocity velocity = ModuleManager.INSTANCE.get(Velocity.class);
        player.setVelocity(player.getVelocity().x + deltaX * velocity.getHorizontal(), player.getVelocity().y + deltaY * velocity.getVertical(), player.getVelocity().z + deltaZ * velocity.getHorizontal());
    }

    @Inject(method = "onItemPickupAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getEntityById(I)Lnet/minecraft/entity/Entity;", ordinal = 0))
    private void onItemPickupAnimation(ItemPickupAnimationS2CPacket packet, CallbackInfo info) {
        Entity itemEntity = client.world.getEntityById(packet.getEntityId());
        Entity entity = client.world.getEntityById(packet.getCollectorEntityId());

        if (itemEntity instanceof ItemEntity && entity == client.player) {
            MeteorClient.EVENT_BUS.post(EventStore.pickItemsEvent(((ItemEntity) itemEntity).getStack(), packet.getStackAmount()));
        }
    }
}
