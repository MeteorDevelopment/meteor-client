package me.jellysquid.mods.lithium.mixin.entity.replace_entitytype_predicates;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.IronGolemWanderAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

@Mixin(IronGolemWanderAroundGoal.class)
public abstract class IronGolemWanderAroundGoalMixin extends WanderAroundGoal {
    public IronGolemWanderAroundGoalMixin(PathAwareEntity mob, double speed) {
        super(mob, speed);
    }

    @Shadow
    protected abstract boolean canVillagerSummonGolem(VillagerEntity villager);

    @Redirect(
            method = "findVillagerPos",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;getEntitiesByType(Lnet/minecraft/util/TypeFilter;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    private List<VillagerEntity> getEntities(ServerWorld serverWorld, TypeFilter<Entity, VillagerEntity> filter, Box box, Predicate<? super VillagerEntity> predicate) {
        if (filter == EntityType.VILLAGER) {
            return serverWorld.getEntitiesByClass(VillagerEntity.class, this.mob.getBoundingBox().expand(32.0), this::canVillagerSummonGolem);
        }
        return serverWorld.getEntitiesByType(EntityType.VILLAGER, this.mob.getBoundingBox().expand(32.0), this::canVillagerSummonGolem);
    }
}
