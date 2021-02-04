package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.render.Freecam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.item.ModelPredicateProviderRegistry$2")
public class ModelPredicateProviderRegistryMixin {
    @Redirect(method = "call", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;yaw:F"))
    private float onCallLivingEntityGetYaw(LivingEntity entity) {
        if (Modules.get().isActive(Freecam.class)) return MinecraftClient.getInstance().gameRenderer.getCamera().getYaw();
        return entity.yaw;
    }

    @Inject(method = "getAngleToPos", at = @At("HEAD"), cancellable = true)
    private void onGetAngleToPos(Vec3d pos, Entity entity, CallbackInfoReturnable<Double> info) {
        if (Modules.get().isActive(Freecam.class)) {
            Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
            info.setReturnValue(Math.atan2(pos.getZ() - camera.getPos().z, pos.getX() - camera.getPos().x));
        }
    }
}
