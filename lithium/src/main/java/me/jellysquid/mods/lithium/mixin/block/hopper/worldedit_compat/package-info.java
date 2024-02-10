@MixinConfigOption(
        description = "Send updates to hoppers when adding inventory block entities to chunks when world edit is loaded. " +
                "Fixes the issue of hoppers not noticing when inventories are placed using worldedit without any block updates.",
        depends = @MixinConfigDependency(dependencyPath = "mixin.util.block_entity_retrieval"),
        enabled = false
)
package me.jellysquid.mods.lithium.mixin.block.hopper.worldedit_compat;

import net.caffeinemc.gradle.MixinConfigDependency;
import net.caffeinemc.gradle.MixinConfigOption;

