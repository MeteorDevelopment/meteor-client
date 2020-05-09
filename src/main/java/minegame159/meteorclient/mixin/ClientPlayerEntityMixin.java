package minegame159.meteorclient.mixin;

import minegame159.meteorclient.CommandDispatcher;
import minegame159.meteorclient.Config;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.Annoy;
import minegame159.meteorclient.modules.player.Portals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Shadow @Final public ClientPlayNetworkHandler networkHandler;

    @Inject(at = @At("HEAD"), method = "sendChatMessage", cancellable = true)
    private void onSendChatMessage(String msg, CallbackInfo info) {
        if (!msg.startsWith(Config.INSTANCE.getPrefix())) {
            if (ModuleManager.INSTANCE.isActive(Annoy.class)) {
                networkHandler.sendPacket(new ChatMessageC2SPacket(ModuleManager.INSTANCE.get(Annoy.class).doAnnoy(msg)));
                info.cancel();
            }

            return;
        }

        info.cancel();
        CommandDispatcher.run(msg.substring(Config.INSTANCE.getPrefix().length()));
    }

    @Redirect(method = "updateNausea", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;"))
    private Screen updateNauseaGetCurrentScreenProxy(MinecraftClient client) {
        if (ModuleManager.INSTANCE.isActive(Portals.class)) return null;
        return client.currentScreen;
    }
}
