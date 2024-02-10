package me.jellysquid.mods.lithium.mixin.util.accessors;

import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerEntityManager.class)
public interface ServerEntityManagerAccessor<T extends EntityLike> {
    @Accessor
    SectionedEntityCache<T> getCache();
}
