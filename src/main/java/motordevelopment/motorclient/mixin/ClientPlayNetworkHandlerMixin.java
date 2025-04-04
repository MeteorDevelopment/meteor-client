/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.commands.Commands;
import motordevelopment.motorclient.events.entity.EntityDestroyEvent;
import motordevelopment.motorclient.events.entity.player.PickItemsEvent;
import motordevelopment.motorclient.events.game.GameJoinedEvent;
import motordevelopment.motorclient.events.game.GameLeftEvent;
import motordevelopment.motorclient.events.game.SendMessageEvent;
import motordevelopment.motorclient.events.packets.ContainerSlotUpdateEvent;
import motordevelopment.motorclient.events.packets.InventoryEvent;
import motordevelopment.motorclient.events.packets.PlaySoundPacketEvent;
import motordevelopment.motorclient.events.world.ChunkDataEvent;
import motordevelopment.motorclient.mixininterface.IExplosionS2CPacket;
import motordevelopment.motorclient.pathing.BaritoneUtils;
import motordevelopment.motorclient.systems.config.Config;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.movement.Velocity;
import motordevelopment.motorclient.systems.modules.render.NoRender;
import motordevelopment.motorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin extends ClientCommonNetworkHandler {
    @Shadow
    private ClientWorld world;

    @Shadow
    public abstract void sendChatMessage(String content);

    @Unique
    private boolean ignoreChatMessage;

    @Unique
    private boolean worldNotNull;

    protected ClientPlayNetworkHandlerMixin(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(method = "onEntitySpawn", at = @At("HEAD"), cancellable = true)
    private void onEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo info) {
        if (packet != null && packet.getEntityType() != null) {
            if (Modules.get().get(NoRender.class).noEntity(packet.getEntityType()) && Modules.get().get(NoRender.class).getDropSpawnPacket()) {
                info.cancel();
            }
        }
    }

    @Inject(method = "onGameJoin", at = @At("HEAD"))
    private void onGameJoinHead(GameJoinS2CPacket packet, CallbackInfo info) {
        worldNotNull = world != null;
    }

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void onGameJoinTail(GameJoinS2CPacket packet, CallbackInfo info) {
        if (worldNotNull) {
            MotorClient.EVENT_BUS.post(GameLeftEvent.get());
        }

        MotorClient.EVENT_BUS.post(GameJoinedEvent.get());
    }

    // the server sends a GameJoin packet after the reconfiguration phase
    @Inject(method = "onEnterReconfiguration", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER))
    private void onEnterReconfiguration(EnterReconfigurationS2CPacket packet, CallbackInfo info) {
        MotorClient.EVENT_BUS.post(GameLeftEvent.get());
    }

    @Inject(method = "onPlaySound", at = @At("HEAD"))
    private void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo info) {
        MotorClient.EVENT_BUS.post(PlaySoundPacketEvent.get(packet));
    }

    @Inject(method = "onChunkData", at = @At("TAIL"))
    private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo info) {
        WorldChunk chunk = client.world.getChunk(packet.getChunkX(), packet.getChunkZ());
        MotorClient.EVENT_BUS.post(new ChunkDataEvent(chunk));
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("TAIL"))
    private void onContainerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo info) {
        MotorClient.EVENT_BUS.post(ContainerSlotUpdateEvent.get(packet));
    }

    @Inject(method = "onInventory", at = @At("TAIL"))
    private void onInventory(InventoryS2CPacket packet, CallbackInfo info) {
        MotorClient.EVENT_BUS.post(InventoryEvent.get(packet));
    }

    @Inject(method = "onEntitiesDestroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/EntitiesDestroyS2CPacket;getEntityIds()Lit/unimi/dsi/fastutil/ints/IntList;"))
    private void onEntitiesDestroy(EntitiesDestroyS2CPacket packet, CallbackInfo ci) {
        for (int id : packet.getEntityIds()) {
            MotorClient.EVENT_BUS.post(EntityDestroyEvent.get(client.world.getEntityById(id)));
        }
    }

    @Inject(method = "onExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER))
    private void onExplosionVelocity(ExplosionS2CPacket packet, CallbackInfo ci) {
        Velocity velocity = Modules.get().get(Velocity.class);
        if (!velocity.explosions.get()) return;

        IExplosionS2CPacket explosionPacket = (IExplosionS2CPacket) (Object) packet;
        explosionPacket.motor$setVelocityX((float) (packet.playerKnockback().orElse(Vec3d.ZERO).x * velocity.getHorizontal(velocity.explosionsHorizontal)));
        explosionPacket.motor$setVelocityY((float) (packet.playerKnockback().orElse(Vec3d.ZERO).y * velocity.getVertical(velocity.explosionsVertical)));
        explosionPacket.motor$setVelocityZ((float) (packet.playerKnockback().orElse(Vec3d.ZERO).z * velocity.getHorizontal(velocity.explosionsHorizontal)));
    }

    @Inject(method = "onItemPickupAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getEntityById(I)Lnet/minecraft/entity/Entity;", ordinal = 0))
    private void onItemPickupAnimation(ItemPickupAnimationS2CPacket packet, CallbackInfo info) {
        Entity itemEntity = client.world.getEntityById(packet.getEntityId());
        Entity entity = client.world.getEntityById(packet.getCollectorEntityId());

        if (itemEntity instanceof ItemEntity && entity == client.player) {
            MotorClient.EVENT_BUS.post(PickItemsEvent.get(((ItemEntity) itemEntity).getStack(), packet.getStackAmount()));
        }
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (ignoreChatMessage) return;

        if (!message.startsWith(Config.get().prefix.get()) && !(BaritoneUtils.IS_AVAILABLE && message.startsWith(BaritoneUtils.getPrefix()))) {
            SendMessageEvent event = MotorClient.EVENT_BUS.post(SendMessageEvent.get(message));

            if (!event.isCancelled()) {
                ignoreChatMessage = true;
                sendChatMessage(event.message);
                ignoreChatMessage = false;
            }
            ci.cancel();
            return;
        }

        if (message.startsWith(Config.get().prefix.get())) {
            try {
                Commands.dispatch(message.substring(Config.get().prefix.get().length()));
            } catch (CommandSyntaxException e) {
                ChatUtils.error(e.getMessage());
            }

            client.inGameHud.getChatHud().addToMessageHistory(message);
            ci.cancel();
        }
    }
}
