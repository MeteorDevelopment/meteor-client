/**
 * This package includes a patch that reduces the memory usage and performance impact of querying the passengers of an
 * entity by replacing Java Stream code with ArrayList operations where possible.
 */
@MixinConfigOption(description = "Reduce stream code usage when getting the passengers of an entity")
package me.jellysquid.mods.lithium.mixin.alloc.deep_passengers;

import net.caffeinemc.gradle.MixinConfigOption;