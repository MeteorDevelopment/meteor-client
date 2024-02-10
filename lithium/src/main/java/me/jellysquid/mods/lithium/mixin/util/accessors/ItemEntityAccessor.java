package me.jellysquid.mods.lithium.mixin.util.accessors;

import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
    @Accessor("owner")
    UUID lithium$getOwner();
}
