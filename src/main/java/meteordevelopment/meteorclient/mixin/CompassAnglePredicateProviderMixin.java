/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import net.minecraft.client.item.CompassAnglePredicateProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(CompassAnglePredicateProvider.class)
public abstract class CompassAnglePredicateProviderMixin {
    @ModifyExpressionValue(method = "getBodyYaw", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getBodyYaw()F"))
    private float callLivingEntityGetYaw(float original) {
        if (Modules.get().isActive(Freecam.class)) return mc.gameRenderer.getCamera().getYaw();
        return original;
    }

    @ModifyReturnValue(method = "getAngleTo(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/BlockPos;)D", at = @At("RETURN"))
    private double modifyGetAngleTo(double original, Entity entity, BlockPos pos) {
        if (Modules.get().isActive(Freecam.class)) {
            Vec3d vec3d = Vec3d.ofCenter(pos);
            Camera camera = mc.gameRenderer.getCamera();
            return Math.atan2(vec3d.getZ() - camera.getPos().z, vec3d.getX() - camera.getPos().x) / (float) (Math.PI * 2);
        }

        return original;
    }
}
