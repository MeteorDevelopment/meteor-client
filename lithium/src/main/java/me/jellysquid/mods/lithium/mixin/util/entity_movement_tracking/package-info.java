@MixinConfigOption(
        description = "System to notify subscribers of certain entity sections about position changes of certain entity types.",
        depends = @MixinConfigDependency(dependencyPath = "mixin.util.entity_section_position")
)
package me.jellysquid.mods.lithium.mixin.util.entity_movement_tracking;

import net.caffeinemc.gradle.MixinConfigDependency;
import net.caffeinemc.gradle.MixinConfigOption;