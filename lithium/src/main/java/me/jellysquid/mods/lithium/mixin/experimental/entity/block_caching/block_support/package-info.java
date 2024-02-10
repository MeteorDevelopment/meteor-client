@MixinConfigOption(description = "Use the block listening system to skip supporting block search (used for honey block pushing, velocity modifiers like soulsand, etc)",
        depends = @MixinConfigDependency(dependencyPath = "mixin.util.block_tracking.block_listening"))
package me.jellysquid.mods.lithium.mixin.experimental.entity.block_caching.block_support;

import net.caffeinemc.gradle.MixinConfigDependency;
import net.caffeinemc.gradle.MixinConfigOption;