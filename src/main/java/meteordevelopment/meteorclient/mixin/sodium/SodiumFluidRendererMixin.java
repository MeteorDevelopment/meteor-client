/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;

// TODO: Sodium
@Mixin(MinecraftClient.class)
//@Mixin(value = FluidRenderer.class, remap = false)
public class SodiumFluidRendererMixin {
    /*@Final
    @Shadow(remap = false)
    private int[] quadColors;

    /**
     * @author Walaryne
     */
    /*@Inject(method = "calculateQuadColors", at = @At("TAIL"), cancellable = true, remap = false)
    private void onCalculateQuadColors(ModelQuadView quad, BlockRenderView world, BlockPos pos, LightPipeline lighter, Direction dir, float brightness, boolean colorized, CallbackInfo info) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.changeLavaColor.get() && !colorized) {
            Arrays.fill(quadColors, ColorARGB.toABGR(ambience.lavaColor.get().getPacked()));
        }
    }*/
}
