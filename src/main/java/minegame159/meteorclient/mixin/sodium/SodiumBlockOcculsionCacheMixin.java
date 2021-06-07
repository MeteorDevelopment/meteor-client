/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin.sodium;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;

// TODO: Sodium
@Mixin(MinecraftClient.class)
//@Mixin(value = BlockOcclusionCache.class, remap = false)
public class SodiumBlockOcculsionCacheMixin {
    /*@Inject(method = "shouldDrawSide", at = @At("RETURN"), cancellable = true)
    private void shouldDrawSide(BlockState state, BlockView view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> info) {
        Xray xray = Modules.get().get(Xray.class);

        if (xray.isActive()) {
            info.setReturnValue(xray.modifyDrawSide(state, view, pos, facing, info.getReturnValueZ()));
        }
    }*/
}
