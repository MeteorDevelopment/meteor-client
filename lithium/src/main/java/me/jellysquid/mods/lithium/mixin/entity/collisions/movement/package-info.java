@MixinConfigOption(
        description = "Entity movement uses optimized block access and optimized and delayed entity access",
        depends = @MixinConfigDependency(dependencyPath = "mixin.util.chunk_access")
)
package me.jellysquid.mods.lithium.mixin.entity.collisions.movement;

import net.caffeinemc.gradle.MixinConfigDependency;
import net.caffeinemc.gradle.MixinConfigOption;