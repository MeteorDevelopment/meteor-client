package me.jellysquid.mods.lithium.mixin.chunk.entity_class_groups;

import me.jellysquid.mods.lithium.common.client.ClientWorldAccessor;
import net.minecraft.client.world.ClientEntityManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientWorld.class)
public class ClientWorldMixin implements ClientWorldAccessor {
    @Shadow
    @Final
    private ClientEntityManager<Entity> entityManager;

    @Override
    public ClientEntityManager<Entity> getEntityManager() {
        return this.entityManager;
    }
}

