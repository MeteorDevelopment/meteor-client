/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.indigo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockRenderInfo.class)
public class BlockRenderInfoMixin {
    @Shadow
    public BlockState blockState;

    @Shadow
    public BlockRenderView blockView;

    @Shadow
    public BlockPos blockPos;

    @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"))
    private boolean modifyShouldDrawSide(boolean original, Direction side) {
        Xray xray = Modules.get().get(Xray.class);

        if (xray.isActive()) {
            return xray.modifyDrawSide(blockState, blockView, blockPos, side, original);
        }

        return original;
    }
}
