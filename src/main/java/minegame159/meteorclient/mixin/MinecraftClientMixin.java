package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.OpenScreenEvent;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements IMinecraftClient {
    @Shadow public ClientWorld world;

    @Shadow private static int currentFps;

    @Shadow private int itemUseCooldown;

    @Shadow protected abstract void doItemUse();

    @Shadow protected abstract void doAttack();

    @Shadow @Final public Mouse mouse;

    @Shadow @Final private Window window;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(RunArgs args, CallbackInfo info) {
        MeteorClient.instance.onInitializeClient();
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo info) {
        if (Utils.canUpdate()) {
            world.getProfiler().swap("meteor-client_update");
            MeteorClient.eventBus.post(EventStore.tickEvent());
        }
    }

    @Inject(method = "openScreen", at = @At("HEAD"), cancellable = true)
    private void onOpenScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof WidgetScreen) screen.mouseMoved(mouse.getX() * window.getScaleFactor(), mouse.getY() * window.getScaleFactor());

        OpenScreenEvent event = EventStore.openScreenEvent(screen);
        MeteorClient.eventBus.post(event);

        if (event.isCancelled()) info.cancel();
    }

    @Override
    public void leftClick() {
        doItemUse();
    }

    @Override
    public void rightClick() {
        doAttack();
    }

    @Override
    public int getCurrentFps() {
        return currentFps;
    }

    @Override
    public void setItemUseCooldown(int cooldown) {
        itemUseCooldown = cooldown;
    }
}
