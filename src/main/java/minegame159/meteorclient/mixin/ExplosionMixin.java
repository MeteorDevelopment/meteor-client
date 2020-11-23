/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.movement.Velocity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Explosion.class)
public class ExplosionMixin {
    private Entity entity;

    @Redirect(method = "collectBlocksAndDamageEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isImmuneToExplosion()Z"))
    private boolean collectBlocksAndDamageEntitiesEntityIsImmuneToExplosionProxy(Entity entity) {
        this.entity = entity;
        return entity.isImmuneToExplosion();
    }

    @Redirect(method = "collectBlocksAndDamageEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d collectBlocksAndDamageEntitiesVec3dAddProxy(Vec3d vec3d, double x, double y, double z) {
        if (!entity.getUuid().equals(MinecraftClient.getInstance().player.getUuid())) return vec3d.add(x, y, z);

        Velocity velocity = ModuleManager.INSTANCE.get(Velocity.class);
        Vec3d newVec3d = vec3d.add(x * velocity.getHorizontal(), y * velocity.getVertical(), z * velocity.getHorizontal());

        entity = null;
        return newVec3d;
    }
}
