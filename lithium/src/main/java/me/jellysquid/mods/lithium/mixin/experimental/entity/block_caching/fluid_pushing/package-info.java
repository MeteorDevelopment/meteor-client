@MixinConfigOption(description = "Use the block listening system to cache entity fluid interaction when not touching fluid currents.",
depends = @MixinConfigDependency(dependencyPath = "mixin.util.block_tracking.block_listening"))
package me.jellysquid.mods.lithium.mixin.experimental.entity.block_caching.fluid_pushing;

import net.caffeinemc.gradle.MixinConfigDependency;
import net.caffeinemc.gradle.MixinConfigOption;