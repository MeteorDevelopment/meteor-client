package me.jellysquid.mods.lithium.mixin.entity.collisions.unpushable_cramming;

import me.jellysquid.mods.lithium.common.entity.pushable.BlockCachingEntity;
import me.jellysquid.mods.lithium.common.entity.pushable.EntityPushablePredicate;
import me.jellysquid.mods.lithium.common.entity.pushable.PushableEntityClassGroup;
import me.jellysquid.mods.lithium.common.util.collections.ReferenceMaskedList;
import me.jellysquid.mods.lithium.common.world.ClimbingMobCachingSection;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.EntityTrackingStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Iterator;

@Mixin(EntityTrackingSection.class)
public abstract class EntityTrackingSectionMixin<T extends EntityLike> implements ClimbingMobCachingSection {
    @Shadow
    @Final
    private TypeFilterableList<T> collection;
    @Shadow
    private EntityTrackingStatus status;

    /**
     * Contains entities that are pushable under some conditions. Entities that are cached to be inside a climbable block
     * and therefore cannot be pushed (only applied to some entity types) are hidden by the mask until the cache is cleared.
     */
    @Unique
    private ReferenceMaskedList<Entity> pushableEntities;

    @Override
    public LazyIterationConsumer.NextIteration collectPushableEntities(World world, Entity except, Box box, EntityPushablePredicate<? super Entity> entityPushablePredicate, ArrayList<Entity> entities) {
        Iterator<?> entityIterator;
        if (this.pushableEntities != null) {
            entityIterator = this.pushableEntities.iterator();
        } else {
            entityIterator = this.collection.iterator();
        }
        int i = 0;
        int j = 0;
        while (entityIterator.hasNext()) {
            Entity entity = (Entity) entityIterator.next();
            if (entity.getBoundingBox().intersects(box) && !entity.isSpectator() && entity != except && !(entity instanceof EnderDragonEntity)) {
                i++;
                if (entityPushablePredicate.test(entity)) { //This predicate has side effects, might cause BlockCachingEntity to cache block and update its visibility
                    j++;
                    //skip the dragon piece check due to dragon pieces always being non pushable
                    entities.add(entity);
                }
            }
        }
        if (this.pushableEntities == null && i >= 25 && i >= (j * 2)) {
            this.startFilteringPushableEntities();
        }
        return LazyIterationConsumer.NextIteration.CONTINUE;
    }

    private void startFilteringPushableEntities() {
        this.pushableEntities = new ReferenceMaskedList<>();
        for (T entity : this.collection) {
            this.onStartClimbingCachingEntity((Entity) entity);
        }
    }

    private void stopFilteringPushableEntities() {
        this.pushableEntities = null;
    }

    //This might be called while the world is in an inconsistent state. E.g. the entity may be in a different section than
    //it is registered to.
    @Override
    public void onEntityModifiedCachedBlock(BlockCachingEntity entity, BlockState newBlockState) {
        if (this.pushableEntities == null) {
            entity.lithiumSetClimbingMobCachingSectionUpdateBehavior(false);
        } else {
            this.updatePushabilityOnCachedStateChange(entity, newBlockState);
        }
    }

    private void updatePushabilityOnCachedStateChange(BlockCachingEntity entity, BlockState newBlockState) {
        boolean visible = entityPushableHeuristic(newBlockState);
        //The entity might be moving into this section right now but isn't registered yet.
        // If the entity is not in the collection, do nothing.
        // When it becomes registered to this section, it will be set to the correct visibility as well.
        this.pushableEntities.setVisible((Entity) entity, visible);
    }

    private void onStartClimbingCachingEntity(Entity entity) {
        Class<? extends Entity> entityClass = entity.getClass();
        if (PushableEntityClassGroup.MAYBE_PUSHABLE.contains(entityClass)) {
            this.pushableEntities.add(entity);
            boolean shouldTrackBlockChanges = PushableEntityClassGroup.CACHABLE_UNPUSHABILITY.contains(entityClass);
            if (shouldTrackBlockChanges) {
                BlockCachingEntity blockCachingEntity = (BlockCachingEntity) entity;
                this.updatePushabilityOnCachedStateChange(blockCachingEntity, blockCachingEntity.getCachedFeetBlockState());
                blockCachingEntity.lithiumSetClimbingMobCachingSectionUpdateBehavior(true);
            }
        }
    }


    @Inject(method = "add(Lnet/minecraft/world/entity/EntityLike;)V", at = @At("RETURN"))
    private void onEntityAdded(T entityLike, CallbackInfo ci) {
        if (this.pushableEntities != null) {
            if (!this.status.shouldTrack()) {
                this.stopFilteringPushableEntities();
            } else {
                this.onStartClimbingCachingEntity((Entity) entityLike);
                if (this.pushableEntities.totalSize() > this.collection.size()) {
                    //Todo: Decide on proper issue handling, printing a warning (?)
                    //something is leaking somewhere, maybe due to mod compat issues!
                    this.stopFilteringPushableEntities();
                }
            }
        }
    }

    @Inject(method = "remove(Lnet/minecraft/world/entity/EntityLike;)Z", at = @At("RETURN"))
    private void onEntityRemoved(T entityLike, CallbackInfoReturnable<Boolean> cir) {
        if (this.pushableEntities != null) {
            if (!this.status.shouldTrack()) {
                this.stopFilteringPushableEntities();
            } else {
                this.pushableEntities.remove((Entity) entityLike);
            }
        }
    }

    /**
     * Whether entities with this feet BlockState should be considered to be pushable. Some entity types are not pushable
     * when they are inside climbable blocks like ladders. Returns true for edge-cases
     * like entity in a trapdoor (which maybe is climbable due to a ladder below).
     *
     * @param cachedFeetBlockState cached BlockState at entity feet
     * @return whether the entity should be treated as pushable
     */
    private static boolean entityPushableHeuristic(BlockState cachedFeetBlockState) {
        return cachedFeetBlockState == null || !cachedFeetBlockState.isIn(BlockTags.CLIMBABLE);
    }
}
