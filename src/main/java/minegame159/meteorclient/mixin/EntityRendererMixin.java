package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.Nametags;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
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

    @Inject(method = "renderLabel(Lnet/minecraft/entity/Entity;Ljava/lang/String;DDDI)V", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(T entity, String text, double x, double y, double z, int maxDistance, CallbackInfo info) {
        if (!(entity instanceof PlayerEntity)) return;
        if (ModuleManager.INSTANCE.isActive(Nametags.class)) info.cancel();
        else return;

        ModuleManager.INSTANCE.get(Nametags.class).render(Math.sqrt(renderManager.getSquaredDistanceToCamera(entity.x, entity.y, entity.z)), entity.getHeight(), x, y, z, renderManager.cameraYaw, renderManager.cameraPitch, entity.getDisplayName().asFormattedString(), (int) ((PlayerEntity) entity).getHealth(), (int) ((PlayerEntity) entity).getMaximumHealth());
    }
}
