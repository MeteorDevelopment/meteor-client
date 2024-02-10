package me.jellysquid.mods.lithium.common.world.interests.iterator;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.function.Predicate;

public record SinglePointOfInterestTypeFilter(
        RegistryEntry<PointOfInterestType> type) implements Predicate<RegistryEntry<PointOfInterestType>> {

    @Override
    public boolean test(RegistryEntry<PointOfInterestType> other) {
        return this.type == other;
    }

    public RegistryEntry<PointOfInterestType> getType() {
        return this.type;
    }
}
