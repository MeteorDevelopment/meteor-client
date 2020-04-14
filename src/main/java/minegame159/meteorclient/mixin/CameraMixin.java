package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.NoClip;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {

    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpace(double desiredCameraDistance, CallbackInfoReturnable<Double> info) {
        if (ModuleManager.INSTANCE.isActive(NoClip.class)) {
            info.setReturnValue(desiredCameraDistance);
        }
    }
}
