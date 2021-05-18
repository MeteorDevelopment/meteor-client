package minegame159.meteorclient.mixin;

import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.world.Ambience;
import net.minecraft.client.color.world.FoliageColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FoliageColors.class)
public class FoliageColorsMixin {

    @Inject(method = "getBirchColor", at = @At("HEAD"), cancellable = true)
    private static void onGetBirchColor(CallbackInfoReturnable<Integer> cir) {
        Ambience ambience = Modules.get().get(Ambience.class);
        if(ambience.isActive() && ambience.changeFoliageColor.get()) {
            cir.setReturnValue(ambience.foliageColor.get().getPacked());
            cir.cancel();
        }
    }

    @Inject(method = "getSpruceColor", at = @At("HEAD"), cancellable = true)
    private static void onGetSpruceColor(CallbackInfoReturnable<Integer> cir) {
        Ambience ambience = Modules.get().get(Ambience.class);
        if(ambience.isActive() && ambience.changeFoliageColor.get()) {
            cir.setReturnValue(ambience.foliageColor.get().getPacked());
            cir.cancel();
        }
    }
}
