/**
 * This package includes a patch that reduces the memory usage and performance impact of Composter usages. The repeated
 * array allocations are replaced by the came copy that is stored in a static final field.
 */
@MixinConfigOption(description = "Composters will reuse the available slot arrays that are requested by hoppers")
package me.jellysquid.mods.lithium.mixin.alloc.composter;

import net.caffeinemc.gradle.MixinConfigOption;