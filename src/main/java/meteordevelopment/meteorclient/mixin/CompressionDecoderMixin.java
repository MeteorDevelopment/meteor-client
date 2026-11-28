/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import net.minecraft.network.CompressionDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CompressionDecoder.class)
public abstract class CompressionDecoderMixin {
    @ModifyExpressionValue(method = "decode", at = @At(value = "CONSTANT", args = "intValue=8388608"))
    private int meteor$maximizeUncompressedPacketLimit(int original) {
        return Modules.get().isActive(AntiPacketKick.class) ? Integer.MAX_VALUE : original;
    }
}
