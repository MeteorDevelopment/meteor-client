/**
 * This package includes a patch that reduces the memory usage and performance impact of explosions by avoiding a lambda
 * allocation in the very often called block blast resistance calculation function
 * {@link net.minecraft.world.explosion.EntityExplosionBehavior#getBlastResistance(net.minecraft.world.explosion.Explosion, net.minecraft.world.BlockView, net.minecraft.util.math.BlockPos, net.minecraft.block.BlockState, net.minecraft.fluid.FluidState)}.
 */
@MixinConfigOption(description = "Remove lambda allocation in frequently called block blast resistance calculation in explosion code")
package me.jellysquid.mods.lithium.mixin.alloc.explosion_behavior;

import net.caffeinemc.gradle.MixinConfigOption;