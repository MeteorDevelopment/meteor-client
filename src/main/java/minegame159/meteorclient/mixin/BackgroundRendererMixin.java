package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.render.AntiFog;
import minegame159.meteorclient.modules.render.XRay;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private void onApplyFog(Camera camera, int i, CallbackInfo info) {
        if (AntiFog.INSTANCE.isActive() || XRay.INSTANCE.isActive()) info.cancel();
    }
}
