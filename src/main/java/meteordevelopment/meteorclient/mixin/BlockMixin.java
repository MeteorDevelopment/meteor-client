/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import meteordevelopment.meteorclient.systems.modules.movement.Slippy;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin extends BlockBehaviour implements ItemLike {
    public BlockMixin(Properties settings) {
        super(settings);
    }

    @ModifyReturnValue(method = "getFriction", at = @At("RETURN"))
    public float getSlipperiness(float original) {
        // For some retarded reason Tweakeroo calls this method before meteor is initialized
        if (Modules.get() == null) return original;

        Slippy slippy = Modules.get().get(Slippy.class);
        Block block = (Block) (Object) this;

        if (slippy.isActive() && (slippy.listMode.get() == Slippy.ListMode.Whitelist ? slippy.allowedBlocks.get().contains(block) : !slippy.ignoredBlocks.get().contains(block))) {
            return slippy.friction.get().floatValue();
        }

        if (block == Blocks.SLIME_BLOCK && Modules.get().get(NoSlow.class).slimeBlock()) return 0.6F;
        else return original;
    }

    // For More Culling compatibility - runs before More Culling's inject to force-render whitelisted Xray blocks
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void meteor$forceXrayFace(BlockState state, BlockState sideState, Direction side, CallbackInfoReturnable<Boolean> cir) {
        Modules modules = Modules.get();
        if (modules == null) return;

        Xray xray = modules.get(Xray.class);
        if (xray.isActive() && !xray.isBlocked(state.getBlock(), null)) {
            cir.setReturnValue(true);
        }
    }
}
