package me.jellysquid.mods.lithium.mixin.entity.replace_entitytype_predicates;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.FormCaravanGoal;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

@Mixin(FormCaravanGoal.class)
public class FormCaravanGoalMixin {
    @Redirect(
            method = "canStart()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getOtherEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    private List<LlamaEntity> getLlamasForCaravan(World world, Entity excluded, Box box, Predicate<? super Entity> predicate) {
        return world.getEntitiesByClass(LlamaEntity.class, box, entity -> entity != excluded);
    }
}
