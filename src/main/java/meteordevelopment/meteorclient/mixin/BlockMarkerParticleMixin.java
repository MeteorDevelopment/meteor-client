package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BarrierTweaks;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.BlockMarkerParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockMarkerParticle.class)
public abstract class BlockMarkerParticleMixin extends Particle {
    protected BlockMarkerParticleMixin(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(ClientWorld world, double x, double y, double z, BlockState state, CallbackInfo ci) {
        BarrierTweaks module = Modules.get().get(BarrierTweaks.class);

        if (module != null && module.isActive() && state.isOf(Blocks.BARRIER)) {
            this.maxAge = module.maxAge.get();
        }
    }
}
