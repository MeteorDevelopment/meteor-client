package me.jellysquid.mods.lithium.mixin.ai.nearby_entity_tracking.goals;

import me.jellysquid.mods.lithium.common.entity.nearby_tracker.NearbyEntityListenerProvider;
import me.jellysquid.mods.lithium.common.entity.nearby_tracker.NearbyEntityTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;

@Mixin(FleeEntityGoal.class)
public class FleeEntityGoalMixin<T extends LivingEntity> {
    @Shadow
    @Final
    protected PathAwareEntity mob;
    @Shadow
    @Final
    protected float fleeDistance;
    private NearbyEntityTracker<T> tracker;

    @Inject(method = "<init>(Lnet/minecraft/entity/mob/PathAwareEntity;Ljava/lang/Class;Ljava/util/function/Predicate;FDDLjava/util/function/Predicate;)V", at = @At("RETURN"))
    private void init(PathAwareEntity mob, Class<T> fleeFromType, Predicate<LivingEntity> predicate, float distance, double slowSpeed, double fastSpeed, Predicate<LivingEntity> predicate2, CallbackInfo ci) {
        EntityDimensions dimensions = this.mob.getType().getDimensions();
        double adjustedRange = dimensions.width * 0.5D + this.fleeDistance + 2D;
        int horizontalRange = MathHelper.ceil(adjustedRange);
        this.tracker = new NearbyEntityTracker<>(fleeFromType, mob, new Vec3i(horizontalRange, MathHelper.ceil(dimensions.height + 3 + 2), horizontalRange));

        ((NearbyEntityListenerProvider) mob).addListener(this.tracker);
    }

    @Redirect(
            method = "canStart()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getClosestEntity(Ljava/util/List;Lnet/minecraft/entity/ai/TargetPredicate;Lnet/minecraft/entity/LivingEntity;DDD)Lnet/minecraft/entity/LivingEntity;"
            )
    )
    private T redirectGetNearestEntity(World world, List<? extends T> entityList, TargetPredicate targetPredicate, LivingEntity entity, double x, double y, double z) {
        return this.tracker.getClosestEntity(this.mob.getBoundingBox().expand(this.fleeDistance, 3.0D, this.fleeDistance), targetPredicate, x, y, z);
    }

    @Redirect(method = "canStart()Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getEntitiesByClass(Ljava/lang/Class;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private <R extends Entity> List<R> redirectGetEntities(World world, Class<T> entityClass, Box box, Predicate<? super R> predicate) {
        return null;
    }
}
