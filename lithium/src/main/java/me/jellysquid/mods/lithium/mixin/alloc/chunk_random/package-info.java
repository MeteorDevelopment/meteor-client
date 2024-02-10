/**
 * This package includes a patch that reduces the memory usage of random position selection when ticking chunks by
 * replacing the repeated BlockPos allocations with a single BlockPos.Mutable that is defensively copied only when
 * passed to unknown code.
 */
@MixinConfigOption(description = "Random block ticking uses fewer block position allocations, thereby reducing the object allocation rate.")
package me.jellysquid.mods.lithium.mixin.alloc.chunk_random;

import net.caffeinemc.gradle.MixinConfigOption;