package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(at = @At("HEAD"), method = "onKey", cancellable = true)
    public void onKeyHead(long window, int key, int scancode, int i, int j, CallbackInfo info) {
        if (Utils.canUpdate() && client.currentScreen == null && !client.isPaused() && i == 1) {
            if (ModuleManager.onKeyPress(key)) info.cancel();
        }
    }

    @Inject(at = @At("TAIL"), method = "onKey")
    public void onKeyTail(long window, int key, int scancode, int i, int j, CallbackInfo info) {
        MeteorClient.eventBus.post(EventStore.keyEvent(key, i == 1));
    }
}
