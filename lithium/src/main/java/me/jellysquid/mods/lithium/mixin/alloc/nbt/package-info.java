/**
 * This package includes a patch that reduces the memory usage and performance impact of NBT tags by replacing the
 * HashMap with a hashmap from the fastutil library.
 */
@MixinConfigOption(description = "NBT tags use a fastutil hashmap instead of a standard HashMap")
package me.jellysquid.mods.lithium.mixin.alloc.nbt;

import net.caffeinemc.gradle.MixinConfigOption;