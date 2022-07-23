/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockOcclusionCache.class, remap = false)
public class SodiumBlockOcculsionCacheMixin {
    @Inject(method = "shouldDrawSide", at = @At("RETURN"), cancellable = true)
    private void shouldDrawSide(BlockState state, BlockView view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> info) {
        Xray xray = Modules.get().get(Xray.class);

        if (xray.isActive()) {
            info.setReturnValue(xray.modifyDrawSide(state, view, pos, facing, info.getReturnValueZ()));
        }
    }
}
