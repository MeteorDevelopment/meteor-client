/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import baritone.api.BaritoneAPI;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.entity.player.SendMovementPacketsEvent;
import minegame159.meteorclient.events.game.SendMessageEvent;
import minegame159.meteorclient.systems.commands.Commands;
import minegame159.meteorclient.systems.config.Config;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.movement.NoSlow;
import minegame159.meteorclient.systems.modules.movement.Scaffold;
import minegame159.meteorclient.systems.modules.movement.Sneak;
import minegame159.meteorclient.systems.modules.movement.Velocity;
import minegame159.meteorclient.systems.modules.player.Portals;
import minegame159.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    @Shadow @Final public ClientPlayNetworkHandler networkHandler;

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(at = @At("HEAD"), method = "sendChatMessage", cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo info) {
        if (!message.startsWith(Config.get().prefix) && !message.startsWith("/") && !message.startsWith(BaritoneAPI.getSettings().prefix.value)) {
            SendMessageEvent event = MeteorClient.EVENT_BUS.post(SendMessageEvent.get(message));

            if (!event.isCancelled()) networkHandler.sendPacket(new ChatMessageC2SPacket(event.message));

            info.cancel();
            return;
        }

        if (message.startsWith(Config.get().prefix)) {
            try {
                Commands.get().dispatch(message.substring(Config.get().prefix.length()));
            } catch (CommandSyntaxException e) {
                ChatUtils.error(e.getMessage());
            }
            info.cancel();
        }
    }

    @Redirect(method = "updateNausea", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;"))
    private Screen updateNauseaGetCurrentScreenProxy(MinecraftClient client) {
        if (Modules.get().isActive(Portals.class)) return null;
        return client.currentScreen;
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean proxy_tickMovement_isUsingItem(ClientPlayerEntity player) {
        if (Modules.get().get(NoSlow.class).items()) return false;
        return player.isUsingItem();
    }

    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void onIsSneaking(CallbackInfoReturnable<Boolean> info) {
        if (Modules.get().isActive(Scaffold.class)) info.setReturnValue(false);
    }

    @Inject(method = "shouldSlowDown", at = @At("HEAD"), cancellable = true)
    private void onShouldSlowDown(CallbackInfoReturnable<Boolean> info) {
        if (Modules.get().get(NoSlow.class).sneaking()) {
            info.setReturnValue(shouldLeaveSwimmingPose());
        }
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocks(double x, double d, CallbackInfo info) {
        Velocity velocity = Modules.get().get(Velocity.class);
        if (velocity.isActive() && velocity.blocks.get()) {
            info.cancel();
        }
    }

    // Rotations

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPacketsHead(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Pre.get());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 0))
    private void onTickHasVehicleBeforeSendPackets(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Pre.get());
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void onSendMovementPacketsTail(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Post.get());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/Packet;)V", ordinal = 1, shift = At.Shift.AFTER))
    private void onTickHasVehicleAfterSendPackets(CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Post.get());
    }

    // Sneak
    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSneaking()Z"))
    private boolean isSneaking(ClientPlayerEntity clientPlayerEntity) {
        return Modules.get().get(Sneak.class).doPacket() || Modules.get().get(NoSlow.class).airStrict() || clientPlayerEntity.isSneaking();
    }
}
