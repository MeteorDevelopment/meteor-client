/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.XRay;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin extends AbstractBlock implements ItemConvertible {
    public BlockMixin(Settings settings) {
        super(settings);
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        if (ModuleManager.INSTANCE.isActive(XRay.class)) return 1;
        return super.getAmbientOcclusionLightLevel(state, world, pos);
    }

    @Inject(at = @At("HEAD"), method = "shouldDrawSide", cancellable = true)
    private static void onShouldDrawSide(BlockState state, BlockView view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> info) {
        XRay xray = ModuleManager.INSTANCE.get(XRay.class);

        if (xray.isActive()) {
            info.setReturnValue(!xray.isBlocked(state.getBlock()));
        }
    }
}
