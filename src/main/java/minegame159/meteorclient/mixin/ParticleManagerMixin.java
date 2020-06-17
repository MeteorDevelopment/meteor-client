package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.NoRender;
import net.minecraft.client.particle.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void onAddParticle(Particle particle, CallbackInfo info) {
        NoRender noRender = ModuleManager.INSTANCE.get(NoRender.class);

        if (noRender.noBubbles() && (particle instanceof BubbleColumnUpParticle || particle instanceof BubblePopParticle || particle instanceof WaterBubbleParticle)) {
            info.cancel();
        } else if (noRender.noExplosion() && (particle instanceof ExplosionSmokeParticle || particle instanceof ExplosionLargeParticle || particle instanceof ExplosionEmitterParticle)) {
            info.cancel();
        }
    }
}
