package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.CharTypedEvent;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    public void onKey(long window, int key, int scancode, int i, int j, CallbackInfo info) {
        if (key != GLFW.GLFW_KEY_UNKNOWN) {
            if (!Utils.canUpdate() && i == 1) {
                MeteorClient.INSTANCE.onKeyInMainMenu(key);
                return;
            }

            if (!client.isPaused() && (client.currentScreen == null || client.currentScreen instanceof WidgetScreen)) {
                KeyEvent event = EventStore.keyEvent(key, i == 1);
                MeteorClient.eventBus.post(event);

                if (event.isCancelled()) info.cancel();
            }
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void onChar(long window, int i, int j, CallbackInfo info) {
        if (Utils.canUpdate() && !client.isPaused() && (client.currentScreen == null || client.currentScreen instanceof WidgetScreen)) {
            CharTypedEvent event = EventStore.charTypedEvent((char) i);
            MeteorClient.eventBus.post(event);

            if (event.isCancelled()) info.cancel();
        }
    }
}
