package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.ESP;
import minegame159.meteorclient.modules.render.Nametags;
import net.minecraft.client.render.VertexConsumerProvider;
import minegame159.meteorclient.utils.Outlines;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(T entity, String string, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
        if (!(entity instanceof PlayerEntity)) return;
        if (ModuleManager.INSTANCE.isActive(Nametags.class)) info.cancel();
    }

    @Inject(method = "getOutlineColor", at = @At("HEAD"), cancellable = true)
    private void onGetOutlineColor(T entity, CallbackInfoReturnable<Integer> info) {
        if (Outlines.renderingOutlines) {
            info.setReturnValue(ModuleManager.INSTANCE.get(ESP.class).getColor(entity).getPacked());
        }
    }
}
