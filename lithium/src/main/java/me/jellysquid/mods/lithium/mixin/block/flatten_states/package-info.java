/**
 * This package includes a patch that stores information about fluid states directly in the FluidState object to improve
 * the performance of accessing whether the FluidState is empty.
 */
@MixinConfigOption(description = "FluidStates store directly whether they are empty")
package me.jellysquid.mods.lithium.mixin.block.flatten_states;

import net.caffeinemc.gradle.MixinConfigOption;