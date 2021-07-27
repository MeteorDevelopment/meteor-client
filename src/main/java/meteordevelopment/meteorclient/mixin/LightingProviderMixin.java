package meteordevelopment.meteorclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import net.minecraft.world.chunk.light.LightingProvider;

@Mixin(LightingProvider.class)
public class LightingProviderMixin {
	
	@Inject(method = "doLightUpdates", at = @At("HEAD"), cancellable = true)
	private void skipLightUpdates(int i, boolean bl, boolean bl2, CallbackInfoReturnable<Integer> cir) {
		Fullbright fb = Modules.get().get(Fullbright.class);
		if(fb.isActive() && fb.disableLightUpdates.get()) {
			cir.setReturnValue(1);
		}
	}
}
