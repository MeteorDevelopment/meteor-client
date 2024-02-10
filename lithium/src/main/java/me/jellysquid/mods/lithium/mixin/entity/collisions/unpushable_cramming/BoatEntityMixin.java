package me.jellysquid.mods.lithium.mixin.entity.collisions.unpushable_cramming;

import com.google.common.base.Predicates;
import me.jellysquid.mods.lithium.common.entity.pushable.EntityPushablePredicate;
import me.jellysquid.mods.lithium.common.world.WorldHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.entity.SectionedEntityCache;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Mixin(BoatEntity.class)
public class BoatEntityMixin {
    @Redirect(
            method = "tick()V",
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
}
