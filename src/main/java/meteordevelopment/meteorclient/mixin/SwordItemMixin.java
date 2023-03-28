/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.SwordItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SwordItem.class)
public class SwordItemMixin {
    @Inject(method = "isSuitableFor", at = @At("RETURN"), cancellable = true)
    private void injected(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(state.isOf(Blocks.COBWEB) || state.isOf(Blocks.BAMBOO) || state.isOf(Blocks.BAMBOO_SAPLING));
    }
}
