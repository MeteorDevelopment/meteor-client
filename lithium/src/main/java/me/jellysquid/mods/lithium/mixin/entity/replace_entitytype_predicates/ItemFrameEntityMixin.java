package me.jellysquid.mods.lithium.mixin.entity.replace_entitytype_predicates;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameEntityMixin extends AbstractDecorationEntity {
    protected ItemFrameEntityMixin(EntityType<? extends AbstractDecorationEntity> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }

    @Redirect(
            method = "canStayAttached()Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getOtherEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    private List<Entity> getAbstractDecorationEntities(World world, Entity excluded, Box box, Predicate<? super Entity> predicate) {
        if (predicate == PREDICATE) {
            //noinspection unchecked,rawtypes
            return (List) world.getEntitiesByClass(AbstractDecorationEntity.class, box, entity -> entity != excluded);

        }

        return world.getOtherEntities(excluded, box, predicate);
    }
}
