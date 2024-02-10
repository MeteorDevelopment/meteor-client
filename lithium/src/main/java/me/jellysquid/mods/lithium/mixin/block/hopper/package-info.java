/**
 * This package includes patches that replace the vanilla hopper code almost completely. Metadata about inventories is
 * stored in their stack lists, hopper cache various frequently used information and several update propagation systems
 * and modification counter comparisons make sure that no outdated cached information is used while avoiding quadratic
 * runtime.
 * <p>
 * TODO: This section can be expended to include a lot more details. The README.md in this package includes a detailed description.
 */
@MixinConfigOption(
        description = "Reduces hopper lag using caching, notification systems and BlockEntity sleeping",
        depends = {
                @MixinConfigDependency(dependencyPath = "mixin.util.entity_movement_tracking"),
                @MixinConfigDependency(dependencyPath = "mixin.util.block_entity_retrieval"),
                @MixinConfigDependency(dependencyPath = "mixin.util.inventory_change_listening"),
                @MixinConfigDependency(dependencyPath = "mixin.util.item_stack_tracking")
        }
)
package me.jellysquid.mods.lithium.mixin.block.hopper;

import net.caffeinemc.gradle.MixinConfigDependency;
import net.caffeinemc.gradle.MixinConfigOption;