package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.CharTypedEvent;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.KeyEvent;
import minegame159.meteorclient.gui.GuiThings;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.mixininterface.IKeyBinding;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.player.InvMove;
import minegame159.meteorclient.utils.Input;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
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
        if (key != GLFW.GLFW_KEY_UNKNOWN && GuiThings.postKeyEvents()) {
            Input.setKeyState(key, i != GLFW.GLFW_RELEASE);

            KeyBinding shulkerPeek = MeteorClient.INSTANCE.shulkerPeek;
            if (shulkerPeek.matchesKey(key, scancode) && (i == GLFW.GLFW_PRESS || i == GLFW.GLFW_REPEAT)) ((IKeyBinding) shulkerPeek).setPressed(true);
            else ((IKeyBinding) shulkerPeek).setPressed(false);

            if (client.currentScreen instanceof WidgetScreen && i == GLFW.GLFW_REPEAT) ((WidgetScreen) client.currentScreen).keyRepeated(key, j);

            if (!Utils.canUpdate() && i == GLFW.GLFW_PRESS) {
                MeteorClient.INSTANCE.onKeyInMainMenu(key);
                if (client.currentScreen instanceof WidgetScreen && GuiThings.postKeyEvents()) {
                    ModuleManager.INSTANCE.onKeyOnlyBinding = true;
                    ModuleManager.INSTANCE.onKey.invoke(EventStore.keyEvent(key, true));
                    ModuleManager.INSTANCE.onKeyOnlyBinding = false;
                }
                return;
            }

            if (!client.isPaused() && (client.currentScreen == null || (client.currentScreen instanceof WidgetScreen && GuiThings.postKeyEvents()))) {
                KeyEvent event = EventStore.keyEvent(key, i == GLFW.GLFW_PRESS);
                MeteorClient.EVENT_BUS.post(event);

                if (event.isCancelled()) info.cancel();
            }
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void onChar(long window, int i, int j, CallbackInfo info) {
        if (Utils.canUpdate() && !client.isPaused() && (client.currentScreen == null || client.currentScreen instanceof WidgetScreen)) {
            CharTypedEvent event = EventStore.charTypedEvent((char) i);
            MeteorClient.EVENT_BUS.post(event);

            if (event.isCancelled()) info.cancel();
        }
    }
}
