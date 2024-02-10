@MixinConfigOption(
        description = "Allow retrieving item entities grouped by item type and count from the world.",
        depends = {
                @MixinConfigDependency(dependencyPath = "mixin.util.accessors"),
                @MixinConfigDependency(dependencyPath = "mixin.util.item_stack_tracking")
        }
)
package me.jellysquid.mods.lithium.mixin.experimental.util.item_entity_by_type;

import net.caffeinemc.gradle.MixinConfigDependency;
import net.caffeinemc.gradle.MixinConfigOption;