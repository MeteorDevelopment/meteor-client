@MixinConfigOption(
        description = "BlockEntity Inventories update their listeners when a comparator is placed near them",
        depends = {
                @MixinConfigDependency(dependencyPath = "mixin.util.block_entity_retrieval")
        }
)
package me.jellysquid.mods.lithium.mixin.util.inventory_comparator_tracking;

import net.caffeinemc.gradle.MixinConfigDependency;
import net.caffeinemc.gradle.MixinConfigOption;