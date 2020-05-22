package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.Nametags;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Shadow @Final protected EntityRenderDispatcher renderManager;

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(T entity, String string, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
        if (!(entity instanceof LivingEntity)) return;
        if (ModuleManager.INSTANCE.isActive(Nametags.class)) info.cancel();
        else return;

        float absorption = ((PlayerEntity) entity).getAbsorptionAmount();

        ModuleManager.INSTANCE.get(Nametags.class).render(matrixStack, Math.sqrt(renderManager.getSquaredDistanceToCamera(entity.getX(), entity.getY(), entity.getZ())), entity.getHeight(), entity.getX() - renderManager.camera.getPos().x, entity.getY() - renderManager.camera.getPos().y, entity.getZ() - renderManager.camera.getPos().z, renderManager.camera.getYaw(), renderManager.camera.getPitch(), entity.getDisplayName().asFormattedString(), (int) (((PlayerEntity) entity).getHealth() + absorption), (int) (((PlayerEntity) entity).getMaximumHealth() + absorption));
    }
}
