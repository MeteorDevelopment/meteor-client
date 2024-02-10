package me.jellysquid.mods.lithium.mixin.ai.nearby_entity_tracking.goals;

import me.jellysquid.mods.lithium.common.entity.nearby_tracker.NearbyEntityListenerProvider;
import me.jellysquid.mods.lithium.common.entity.nearby_tracker.NearbyEntityTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
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

@Mixin(LookAtEntityGoal.class)
public class LookAtEntityGoalMixin {
    @Shadow
    @Final
    protected MobEntity mob;
    @Shadow
    @Final
    protected float range;
    private NearbyEntityTracker<? extends LivingEntity> tracker;

    @Inject(method = "<init>(Lnet/minecraft/entity/mob/MobEntity;Ljava/lang/Class;FFZ)V", at = @At("RETURN"))
    private void init(MobEntity mob, Class<? extends LivingEntity> targetType, float range, float chance, boolean b, CallbackInfo ci) {
        EntityDimensions dimensions = this.mob.getType().getDimensions();
        double adjustedRange = dimensions.width * 0.5D + this.range + 2D;
        int horizontalRange = MathHelper.ceil(adjustedRange);
        this.tracker = new NearbyEntityTracker<>(targetType, mob, new Vec3i(horizontalRange, MathHelper.ceil(dimensions.height + 3 + 2), horizontalRange));

        ((NearbyEntityListenerProvider) mob).addListener(this.tracker);
    }

    @Redirect(
            method = "canStart()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getClosestEntity(Ljava/util/List;Lnet/minecraft/entity/ai/TargetPredicate;Lnet/minecraft/entity/LivingEntity;DDD)Lnet/minecraft/entity/LivingEntity;"
            )
    )
    private LivingEntity redirectGetNearestEntity(World world, List<LivingEntity> entityList, TargetPredicate targetPredicate, LivingEntity entity, double x, double y, double z) {
        return this.tracker.getClosestEntity(this.mob.getBoundingBox().expand(this.range, 3.0D, this.range), targetPredicate, x, y, z);
    }

    @Redirect(method = "canStart()Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getEntitiesByClass(Ljava/lang/Class;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private <R extends Entity> List<R> redirectGetEntities(World world, Class<LivingEntity> entityClass, Box box, Predicate<? super R> predicate) {
        return null;
    }

    @Redirect(
            method = "canStart()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getClosestPlayer(Lnet/minecraft/entity/ai/TargetPredicate;Lnet/minecraft/entity/LivingEntity;DDD)Lnet/minecraft/entity/player/PlayerEntity;"
            )
    )
    private PlayerEntity redirectGetClosestPlayer(World world, TargetPredicate targetPredicate, LivingEntity entity, double x, double y, double z) {
        return (PlayerEntity) this.tracker.getClosestEntity(null, targetPredicate, x, y, z);
    }
}
