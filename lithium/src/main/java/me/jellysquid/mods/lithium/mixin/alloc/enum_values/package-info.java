/**
 * This package includes patches that reduce the memory usage and performance impact of Enum usages. The defensive copy
 * in Enum values() function is avoided by reusing the came copy that is stored in a static final field.
 */
@MixinConfigOption(description = "Avoid `Enum#values()` array copy in frequently called code")
package me.jellysquid.mods.lithium.mixin.alloc.enum_values;

import net.caffeinemc.gradle.MixinConfigOption;