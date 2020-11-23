/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.CommandDispatcher;
import minegame159.meteorclient.Config;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.SendMessageEvent;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.movement.NoSlow;
import minegame159.meteorclient.modules.movement.Scaffold;
import minegame159.meteorclient.modules.player.Portals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Shadow @Final public ClientPlayNetworkHandler networkHandler;

    @Shadow public abstract void sendChatMessage(String string);

    private boolean ignoreChatMessage;

    @Inject(at = @At("HEAD"), method = "sendChatMessage", cancellable = true)
    private void onSendChatMessage(String msg, CallbackInfo info) {
        if (ignoreChatMessage) return;

        if (!msg.startsWith(Config.INSTANCE.getPrefix()) && !msg.startsWith("/")) {
            SendMessageEvent event = EventStore.sendMessageEvent(msg);
            MeteorClient.EVENT_BUS.post(event);

            ignoreChatMessage = true;
            sendChatMessage(event.msg);
            ignoreChatMessage = false;

            info.cancel();
            return;
        }

        if (msg.startsWith(Config.INSTANCE.getPrefix())) {
            CommandDispatcher.run(msg.substring(Config.INSTANCE.getPrefix().length()));
            info.cancel();
        }
    }

    @Redirect(method = "updateNausea", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;"))
    private Screen updateNauseaGetCurrentScreenProxy(MinecraftClient client) {
        if (ModuleManager.INSTANCE.isActive(Portals.class)) return null;
        return client.currentScreen;
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean proxy_tickMovement_isUsingItem(ClientPlayerEntity player) {
        if (ModuleManager.INSTANCE.get(NoSlow.class).items()) return false;
        return player.isUsingItem();
    }

    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void onIsSneaking(CallbackInfoReturnable<Boolean> info) {
        if (ModuleManager.INSTANCE.isActive(Scaffold.class)) info.setReturnValue(false);
    }
}
