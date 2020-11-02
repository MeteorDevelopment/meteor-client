package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.OpenScreenEvent;
import minegame159.meteorclient.gui.GuiKeyEvents;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.mixininterface.IMinecraftClient;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.player.AutoEat;
import minegame159.meteorclient.modules.player.AutoGap;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.Session;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.net.Proxy;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements IMinecraftClient {
    @Shadow public ClientWorld world;

    @Shadow private int itemUseCooldown;

    @Shadow protected abstract void doItemUse();

    @Shadow protected abstract void doAttack();

    @Shadow @Final public Mouse mouse;

    @Shadow @Final private Window window;

    @Shadow @Final private Proxy netProxy;

    @Shadow @Final @Mutable private Session session;

    @Shadow private static int currentFps;

    @Shadow @Nullable public Screen currentScreen;

    @Shadow @Nullable public abstract ServerInfo getCurrentServerEntry();

    private boolean couldUpdate;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        MeteorClient.INSTANCE.onInitializeClient();
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void onPreTick(CallbackInfo info) {
        if (Utils.canUpdate()) {
            world.getProfiler().swap("meteor-client_pre_update");
            MeteorClient.EVENT_BUS.post(EventStore.preTickEvent());
        }
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo info) {
        if (Utils.lastServerInfo == null) Utils.lastServerInfo = getCurrentServerEntry();
        else if (getCurrentServerEntry() != null) Utils.lastServerInfo.copyFrom(getCurrentServerEntry());

        boolean canUpdate = Utils.canUpdate();
        if (canUpdate && !couldUpdate) MeteorClient.EVENT_BUS.post(EventStore.gameJoinedEvent());
        else if (!canUpdate && couldUpdate) MeteorClient.EVENT_BUS.post(EventStore.gameDisconnectedEvent());
        couldUpdate = canUpdate;

        if (canUpdate) {
            world.getProfiler().swap("meteor-client_post_update");
            MeteorClient.EVENT_BUS.post(EventStore.postTickEvent());
        }
    }

    @Inject(method = "openScreen", at = @At("HEAD"), cancellable = true)
    private void onOpenScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof WidgetScreen) screen.mouseMoved(mouse.getX() * window.getScaleFactor(), mouse.getY() * window.getScaleFactor());

        OpenScreenEvent event = EventStore.openScreenEvent(screen);
        MeteorClient.EVENT_BUS.post(event);

        if (event.isCancelled()) {
            info.cancel();
            return;
        }

        GuiKeyEvents.resetPostKeyEvents();
    }

    @Redirect(method = "doItemUse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;crosshairTarget:Lnet/minecraft/util/hit/HitResult;", ordinal = 1))
    private HitResult doItemUseMinecraftClientCrosshairTargetProxy(MinecraftClient client) {
        if (ModuleManager.INSTANCE.get(AutoEat.class).rightClickThings() && ModuleManager.INSTANCE.get(AutoGap.class).rightClickThings()) return client.crosshairTarget;
        return null;
    }

    @Override
    public void leftClick() {
        doAttack();
    }

    @Override
    public void rightClick() {
        doItemUse();
    }

    @Override
    public void setItemUseCooldown(int cooldown) {
        itemUseCooldown = cooldown;
    }

    @Override
    public Proxy getProxy() {
        return netProxy;
    }

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public int getFps() {
        return currentFps;
    }
}
