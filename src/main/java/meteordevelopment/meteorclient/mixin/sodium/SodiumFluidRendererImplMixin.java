/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = FluidRendererImpl.class, remap = false)
public abstract class SodiumFluidRendererImplMixin {
}
