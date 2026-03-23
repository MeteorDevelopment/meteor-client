/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Hitboxes;
import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {
    @ModifyExpressionValue(method = "drawHitbox", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getBoundingBox()Lnet/minecraft/util/math/Box;", ordinal = 0))
    private Box meteor$createHitbox(Box original, Entity entity, float tickProgress, boolean inLocalServer) {
        double v = Modules.get().get(Hitboxes.class).getEntityValue(entity);
        if (v == 0) return original;

        return original.expand(v);
    }
}
