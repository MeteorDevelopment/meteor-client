/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import net.minecraft.network.PacketByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PacketByteBuf.class)
public class PacketByteBufMixin {
    @ModifyConstant(method = "readCompoundTag",constant = @Constant(longValue = 2097152L))
    private long increaseLimit(long old) {
        return 2000000000L;
    }
}