package me.jellysquid.mods.lithium.mixin.ai.nearby_entity_tracking;

import me.jellysquid.mods.lithium.common.entity.nearby_tracker.NearbyEntityListenerMulti;
import me.jellysquid.mods.lithium.common.entity.nearby_tracker.NearbyEntityListenerProvider;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerEntityManager.class)
public abstract class ServerEntityManagerMixin<T extends EntityLike> {
    @Shadow
    @Final
    SectionedEntityCache<T> cache;

    @Inject(
            method = "addEntity(Lnet/minecraft/world/entity/EntityLike;Z)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityLike;setChangeListener(Lnet/minecraft/world/entity/EntityChangeListener;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onAddEntity(T entity, boolean existing, CallbackInfoReturnable<Boolean> cir) {
        NearbyEntityListenerMulti listener = ((NearbyEntityListenerProvider) entity).getListener();
        if (listener != null) {
            listener.addToAllChunksInRange(
                    this.cache,
                    ChunkSectionPos.from(entity.getBlockPos())
            );
        }
    }
}
