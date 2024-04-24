/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.block.AbstractBlock;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Random;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {
    @Unique
    private static final Random RANDOM = new Random();

    @ModifyVariable(method = "getModelOffset", at = @At("HEAD"), argsOnly = true)
    private BlockPos modifyPos(BlockPos pos) {
        if (Modules.get() == null) return pos;

        if (Modules.get().get(NoRender.class).noTextureRotations()) return pos.multiply(RANDOM.nextInt());
        return pos;
    }
}
