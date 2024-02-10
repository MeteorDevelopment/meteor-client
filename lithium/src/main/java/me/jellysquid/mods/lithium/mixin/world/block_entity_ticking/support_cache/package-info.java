@MixinConfigOption(
        description = "BlockEntity ticking caches whether the BlockEntity can exist in the BlockState at the same location",
        enabled = false //have to check whether the cached state bugfix fixes any detectable vanilla bugs first
)
package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.support_cache;

import net.caffeinemc.gradle.MixinConfigOption;