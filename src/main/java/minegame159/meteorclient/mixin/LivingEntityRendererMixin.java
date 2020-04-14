package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.Chams;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin<T extends LivingEntity> {
    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;DDDFF)V", at = @At("HEAD"))
    private void onRenderHead(T livingEntity, double d, double e, double f, float g, float h, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(Chams.class).shouldRender(livingEntity)) {
            GL11.glEnable(32823);
            GL11.glPolygonOffset(1, -1000000);
        }
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;DDDFF)V", at = @At("TAIL"))
    private void onRenderTail(T livingEntity, double d, double e, double f, float g, float h, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(Chams.class).shouldRender(livingEntity)) {
            GL11.glPolygonOffset(1, 1000000);
            GL11.glDisable(32823);
        }
    }
}
