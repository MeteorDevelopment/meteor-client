package me.jellysquid.mods.lithium.mixin.entity.collisions.unpushable_cramming;

import com.google.common.base.Predicates;
import me.jellysquid.mods.lithium.common.entity.pushable.BlockCachingEntity;
import me.jellysquid.mods.lithium.common.entity.pushable.EntityPushablePredicate;
import me.jellysquid.mods.lithium.common.world.ClimbingMobCachingSection;
import me.jellysquid.mods.lithium.common.world.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements BlockCachingEntity {

    boolean updateClimbingMobCachingSectionOnChange;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Redirect(
            method = "tickCramming()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getOtherEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    private List<Entity> getOtherPushableEntities(World world, @Nullable Entity except, Box box, Predicate<? super Entity> predicate) {
        //noinspection Guava
        if (predicate == Predicates.alwaysFalse()) {
            return Collections.emptyList();
        }
        if (predicate instanceof EntityPushablePredicate<?> entityPushablePredicate) {
            SectionedEntityCache<Entity> cache = WorldHelper.getEntityCacheOrNull(world);
            if (cache != null) {
                //noinspection unchecked
                return WorldHelper.getPushableEntities(world, cache, except, box, (EntityPushablePredicate<? super Entity>) entityPushablePredicate);
            }
        }
        return world.getOtherEntities(except, box, predicate);
    }

    @Override
    public void lithiumSetClimbingMobCachingSectionUpdateBehavior(boolean listenForCachedBlockChanges) {
        this.updateClimbingMobCachingSectionOnChange = listenForCachedBlockChanges;
    }

    @Override
    public void lithiumOnBlockCacheDeleted() {
        if (this.updateClimbingMobCachingSectionOnChange) {
            this.updateClimbingMobCachingSection(null);
        }
    }


    @Override
    public void lithiumOnBlockCacheSet(BlockState newState) {
        if (this.updateClimbingMobCachingSectionOnChange) {
            this.updateClimbingMobCachingSection(newState);
        }
    }

    private void updateClimbingMobCachingSection(BlockState newState) {
        SectionedEntityCache<Entity> entityCacheOrNull = WorldHelper.getEntityCacheOrNull(this.getWorld());
        if (entityCacheOrNull != null) {
            EntityTrackingSection<Entity> trackingSection = entityCacheOrNull.findTrackingSection(ChunkSectionPos.toLong(this.getBlockPos()));
            if (trackingSection != null) {
                ((ClimbingMobCachingSection) trackingSection).onEntityModifiedCachedBlock(this, newState);
            } else {
                this.updateClimbingMobCachingSectionOnChange = false;
            }
        }
    }
}
