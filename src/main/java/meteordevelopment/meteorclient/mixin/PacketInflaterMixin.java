/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import net.minecraft.network.PacketInflater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PacketInflater.class)
public class PacketInflaterMixin {
    @ModifyConstant(method = "decode", constant = @Constant(intValue = 8388608))
    private int maxValue(int value) {
        return Modules.get().isActive(AntiPacketKick.class) ? Integer.MAX_VALUE : value;
    }
}
