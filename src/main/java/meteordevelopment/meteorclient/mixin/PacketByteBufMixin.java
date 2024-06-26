/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PacketByteBuf.class)
public abstract class PacketByteBufMixin {
    @ModifyArg(method = "readNbt(Lio/netty/buffer/ByteBuf;)Lnet/minecraft/nbt/NbtCompound;", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketByteBuf;readNbt(Lio/netty/buffer/ByteBuf;Lnet/minecraft/nbt/NbtSizeTracker;)Lnet/minecraft/nbt/NbtElement;"))
    private static NbtSizeTracker xlPackets(NbtSizeTracker sizeTracker) {
        return Modules.get().isActive(AntiPacketKick.class) ? NbtSizeTracker.ofUnlimitedBytes() : sizeTracker;
    }
}
