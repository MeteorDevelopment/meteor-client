/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.GpuTexture;
import meteordevelopment.meteorclient.mixininterface.IGpuTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GpuTexture.class)
public abstract class GpuTextureMixin implements IGpuTexture {
    @Shadow(remap = false)
    protected AddressMode addressModeU;

    @Shadow(remap = false)
    protected AddressMode addressModeV;

    @Override
    public AddressMode meteor$getAddressModeU() {
        return this.addressModeU;
    }

    @Override
    public AddressMode meteor$getAddressModeV() {
        return this.addressModeV;
    }
}
