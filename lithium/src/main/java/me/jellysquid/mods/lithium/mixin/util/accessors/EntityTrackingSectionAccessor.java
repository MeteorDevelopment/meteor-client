package me.jellysquid.mods.lithium.mixin.util.accessors;

import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.world.entity.EntityTrackingSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityTrackingSection.class)
public interface EntityTrackingSectionAccessor<T> {
    @Accessor("collection")
    TypeFilterableList<T> getCollection();
}
