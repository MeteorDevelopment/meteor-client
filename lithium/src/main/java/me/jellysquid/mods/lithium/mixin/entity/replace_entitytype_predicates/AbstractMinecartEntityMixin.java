package me.jellysquid.mods.lithium.mixin.entity.replace_entitytype_predicates;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(AbstractMinecartEntity.class)
public class AbstractMinecartEntityMixin {

    @Redirect(
            method = "tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getOtherEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Ljava/util/List;"
            )
    )
    private List<AbstractMinecartEntity> getOtherAbstractMinecarts(World world, Entity except, Box box) {
        return world.getEntitiesByClass(AbstractMinecartEntity.class, box, entity -> entity != except);
    }
}
