@MixinConfigOption(description = "Use the block listening system to skip block touching (like cactus touching).",
depends = @MixinConfigDependency(dependencyPath = "mixin.util.block_tracking.block_listening"))
package me.jellysquid.mods.lithium.mixin.experimental.entity.block_caching.block_touching;

import net.caffeinemc.gradle.MixinConfigDependency;
import net.caffeinemc.gradle.MixinConfigOption;