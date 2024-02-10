package me.jellysquid.mods.lithium.common.client;

import net.minecraft.client.world.ClientEntityManager;
import net.minecraft.entity.Entity;

public interface ClientWorldAccessor {
    ClientEntityManager<Entity> getEntityManager();
}
