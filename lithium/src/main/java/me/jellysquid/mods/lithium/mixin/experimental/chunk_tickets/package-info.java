@MixinConfigOption(description = "Only check positions with expiring tickets during ticket expiration. Can cause reordering of chunk unloading when unloading more than approximately two billion chunks at once.")
package me.jellysquid.mods.lithium.mixin.experimental.chunk_tickets;
import net.caffeinemc.gradle.MixinConfigOption;