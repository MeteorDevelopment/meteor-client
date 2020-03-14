package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.events.RenderFogEvent;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.XRay;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    private XRay xray;

    private XRay getXray() {
        if (xray == null) xray = ModuleManager.INSTANCE.get(XRay.class);
        return xray;
    }

    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private void onApplyFog(Camera camera, int i, CallbackInfo info) {
        RenderFogEvent event = EventStore.renderFogEvent();
        MeteorClient.eventBus.post(event);

        if (event.isCancelled() || getXray().isActive()) info.cancel();
    }
}
