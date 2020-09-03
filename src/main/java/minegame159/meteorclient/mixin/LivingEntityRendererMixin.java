package minegame159.meteorclient.mixin;

import minegame159.meteorclient.utils.MyRenderLayer;
import minegame159.meteorclient.utils.Outlines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements FeatureRendererContext<T, M> {
    public LivingEntityRendererMixin(EntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
    private void onGetRenderLayer(T entity, boolean showBody, boolean translucent, CallbackInfoReturnable<RenderLayer> info) {
        if (Outlines.renderingOutlines) {
            info.setReturnValue(MyRenderLayer.getOutlineRenderLayer(getTexture(entity)));
        }
    }
}
