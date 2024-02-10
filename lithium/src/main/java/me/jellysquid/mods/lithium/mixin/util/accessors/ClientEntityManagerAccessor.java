package me.jellysquid.mods.lithium.mixin.util.accessors;

import net.minecraft.client.world.ClientEntityManager;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientEntityManager.class)
public interface ClientEntityManagerAccessor<T extends EntityLike> {
    @Accessor
    SectionedEntityCache<T> getCache();
}
