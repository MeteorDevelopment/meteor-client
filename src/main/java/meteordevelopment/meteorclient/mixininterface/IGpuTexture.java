/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

import com.mojang.blaze3d.textures.AddressMode;

public interface IGpuTexture {
    AddressMode  meteor$getAddressModeU();
    AddressMode  meteor$getAddressModeV();
}
