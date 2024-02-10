@MixinConfigOption(
        description = """
                A number of AI goals which query for nearby entities in the world every tick will use the event-based
                system for tracking nearby entities. In other words, instead of entities constantly polling to see if
                other entities are nearby, they will instead be notified only occasionally when such an entity enters
                their range.
                """
)
package me.jellysquid.mods.lithium.mixin.ai.nearby_entity_tracking.goals;

import net.caffeinemc.gradle.MixinConfigOption;