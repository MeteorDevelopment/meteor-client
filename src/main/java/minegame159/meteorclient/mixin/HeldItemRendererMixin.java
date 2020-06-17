package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.NoRender;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderFireOverlay(CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noFireOverlay()) info.cancel();
    }

    @Inject(method = "renderWaterOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderWaterOverlay(float f, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noWaterOverlay()) info.cancel();
    }
}
