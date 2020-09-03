package minegame159.meteorclient.mixin;

import minegame159.meteorclient.utils.MyRenderLayer;
import minegame159.meteorclient.utils.Outlines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderLayer.class)
public class RenderLayerMixin {
    @Inject(method = "getEntityCutout", at = @At("HEAD"), cancellable = true)
    private static void onGetEntityCutout(Identifier texture, CallbackInfoReturnable<RenderLayer> info) {
        if (Outlines.renderingOutlines) {
            info.setReturnValue(MyRenderLayer.getOutlineRenderLayer(texture));
        }
    }
}
