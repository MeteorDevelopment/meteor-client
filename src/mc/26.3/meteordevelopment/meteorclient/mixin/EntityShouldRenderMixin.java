/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The arguments of EntityRenderer#shouldRender are not the same in every version, so this
 * injection lives in the version tree instead of the shared sources.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityShouldRenderMixin<T extends Entity> {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(T entity, Frustum culler, double camX, double camY, double camZ, float partialTick, CallbackInfoReturnable<Boolean> cir) {
        NoRender noRender = Modules.get().get(NoRender.class);

        if (noRender.noEntity(entity)) cir.setReturnValue(false);
        if (noRender.noFallingBlocks() && entity instanceof FallingBlockEntity) cir.setReturnValue(false);
    }
}
