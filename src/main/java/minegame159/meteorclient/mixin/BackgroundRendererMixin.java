package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.modules.ModuleManager;
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
    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void onApplyFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, CallbackInfo info) {
        if (ModuleManager.INSTANCE.isActive(AntiFog.class) || ModuleManager.INSTANCE.isActive(XRay.class)) {
            GlStateManager.disableFog();
        }
    }
}
