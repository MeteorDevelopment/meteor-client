/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.events.entity.EntityDestroyEvent;
import meteordevelopment.meteorclient.events.entity.player.PickItemsEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.packets.ContainerSlotUpdateEvent;
import meteordevelopment.meteorclient.events.packets.InventoryEvent;
import meteordevelopment.meteorclient.events.packets.PlaySoundPacketEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.mixininterface.IExplosionS2CPacket;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import meteordevelopment.meteorclient.systems.modules.player.NoRotate;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin extends ClientCommonPacketListenerImpl {
    @Shadow
    private ClientLevel level;

    protected ClientPlayNetworkHandlerMixin(Minecraft client, Connection connection, CommonListenerCookie connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(method = "handleAddEntity", at = @At("HEAD"), cancellable = true)
    private void onEntitySpawn(ClientboundAddEntityPacket packet, CallbackInfo info) {
        if (packet != null && packet.getType() != null) {
            if (Modules.get().get(NoRender.class).noEntity(packet.getType()) && Modules.get().get(NoRender.class).getDropSpawnPacket()) {
                info.cancel();
            }
        }
    }

    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void onGameJoinHead(ClientboundLoginPacket packet, CallbackInfo info, @Share("worldNotNull") LocalBooleanRef worldNotNull) {
        worldNotNull.set(level != null);
    }

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void onGameJoinTail(ClientboundLoginPacket packet, CallbackInfo info, @Share("worldNotNull") LocalBooleanRef worldNotNull) {
        if (worldNotNull.get()) {
            MeteorClient.EVENT_BUS.post(GameLeftEvent.get());
        }

        MeteorClient.EVENT_BUS.post(GameJoinedEvent.get());
    }

    // the server sends a GameJoin packet after the reconfiguration phase
    @Inject(method = "handleConfigurationStart", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void onEnterReconfiguration(ClientboundStartConfigurationPacket packet, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(GameLeftEvent.get());
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void onPlaySound(ClientboundSoundPacket packet, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(PlaySoundPacketEvent.get(packet));
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At("TAIL"))
    private void onChunkData(ClientboundLevelChunkWithLightPacket packet, CallbackInfo info) {
        LevelChunk chunk = minecraft.level.getChunk(packet.getX(), packet.getZ());
        MeteorClient.EVENT_BUS.post(new ChunkDataEvent(chunk));
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void onContainerSlotUpdate(ClientboundContainerSetSlotPacket packet, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(ContainerSlotUpdateEvent.get(packet));
    }

    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void onInventory(ClientboundContainerSetContentPacket packet, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(InventoryEvent.get(packet));
    }

    @Inject(method = "handleRemoveEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundRemoveEntitiesPacket;getEntityIds()Lit/unimi/dsi/fastutil/ints/IntList;"))
    private void onEntitiesDestroy(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        for (int id : packet.getEntityIds()) {
            MeteorClient.EVENT_BUS.post(EntityDestroyEvent.get(minecraft.level.getEntity(id)));
        }
    }

    @Inject(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void onExplosionVelocity(ClientboundExplodePacket packet, CallbackInfo ci) {
        Velocity velocity = Modules.get().get(Velocity.class);
        if (!velocity.explosions.get()) return;

        IExplosionS2CPacket explosionPacket = (IExplosionS2CPacket) (Object) packet;
        explosionPacket.meteor$setVelocityX((float) (packet.playerKnockback().orElse(Vec3.ZERO).x * velocity.getHorizontal(velocity.explosionsHorizontal)));
        explosionPacket.meteor$setVelocityY((float) (packet.playerKnockback().orElse(Vec3.ZERO).y * velocity.getVertical(velocity.explosionsVertical)));
        explosionPacket.meteor$setVelocityZ((float) (packet.playerKnockback().orElse(Vec3.ZERO).z * velocity.getHorizontal(velocity.explosionsHorizontal)));
    }

    @Inject(method = "handleTakeItemEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void onItemPickupAnimation(ClientboundTakeItemEntityPacket packet, CallbackInfo info) {
        Entity itemEntity = minecraft.level.getEntity(packet.getItemId());
        Entity entity = minecraft.level.getEntity(packet.getPlayerId());

        if (itemEntity instanceof ItemEntity && entity == minecraft.player) {
            MeteorClient.EVENT_BUS.post(PickItemsEvent.get(((ItemEntity) itemEntity).getItem(), packet.getAmount()));
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void onPlayerPositionLookHead(ClientboundPlayerPositionPacket packet, CallbackInfo ci,
          @Share("noRotateYaw") LocalFloatRef yawRef,
          @Share("noRotatePitch") LocalFloatRef pitchRef) {
        NoRotate noRotate = Modules.get().get(NoRotate.class);
        if (!noRotate.isActive() || minecraft.player == null) return;

        yawRef.set(minecraft.player.getYRot());
        pitchRef.set(minecraft.player.getXRot());
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void onPlayerPositionLookReturn(ClientboundPlayerPositionPacket packet, CallbackInfo ci,
        @Share("noRotateYaw") LocalFloatRef yawRef,
        @Share("noRotatePitch") LocalFloatRef pitchRef) {
        NoRotate noRotate = Modules.get().get(NoRotate.class);
        if (!noRotate.isActive() || minecraft.player == null) return;

        float savedYaw = yawRef.get();
        float savedPitch = pitchRef.get();

        //not noticeable by player but forces a server update
        minecraft.player.setYRot(savedYaw + 0.000001f);
        minecraft.player.setXRot(savedPitch + 0.000001f);
        minecraft.player.yHeadRot = savedYaw;
        minecraft.player.yBodyRot = savedYaw;
    }

    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci, @Local(argsOnly = true) LocalRef<String> messageRef) {
        if (!message.startsWith(Config.get().prefix.get()) && !(BaritoneUtils.IS_AVAILABLE && message.startsWith(BaritoneUtils.getPrefix()))) {
            SendMessageEvent event = MeteorClient.EVENT_BUS.post(SendMessageEvent.get(message));

            if (!event.isCancelled()) {
                messageRef.set(event.message);
            } else {
                ci.cancel();
            }

            return;
        }

        if (message.startsWith(Config.get().prefix.get())) {
            try {
                Commands.dispatch(message.substring(Config.get().prefix.get().length()));
            } catch (CommandSyntaxException e) {
                ChatUtils.error(e.getMessage());
            }

            minecraft.gui.getChat().addRecentChat(message);
            ci.cancel();
        }
    }
}
