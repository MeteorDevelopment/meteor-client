package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.AntiFog;
import minegame159.meteorclient.modules.render.XRay;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {
    private AntiFog antiFog;
    private XRay xRay;

    private AntiFog getAntiFog() {
        if (antiFog == null) antiFog = ModuleManager.INSTANCE.get(AntiFog.class);
        return antiFog;
    }

    private XRay getXRay() {
        if (xRay == null) xRay = ModuleManager.INSTANCE.get(XRay.class);
        return xRay;
    }

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpace(double desiredCameraDistance, CallbackInfoReturnable<Double> info) {
        if (getAntiFog().isActive() || getXRay().isActive()) {
            info.setReturnValue(desiredCameraDistance);
        }
    }
}
