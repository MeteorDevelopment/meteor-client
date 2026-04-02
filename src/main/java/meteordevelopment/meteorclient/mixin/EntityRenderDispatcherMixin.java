/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow
    public Camera camera;

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private <S extends EntityRenderState> void render(S renderState, CameraRenderState cameraRenderState, double d, double e, double f, PoseStack matrixStack, SubmitNodeCollector orderedRenderCommandQueue, CallbackInfo ci) {
        var entity = ((IEntityRenderState) renderState).meteor$getEntity();

        if (entity instanceof FakePlayerEntity player && player.hideWhenInsideCamera) {
            int cX = Mth.floor(this.camera.position().x);
            int cY = Mth.floor(this.camera.position().y);
            int cZ = Mth.floor(this.camera.position().z);

            if (cX == entity.getBlockX() && cZ == entity.getBlockZ() && (cY == entity.getBlockY() || cY == entity.getBlockY() + 1))
                ci.cancel();
        }
    }

    // IEntityRenderState

    @ModifyExpressionValue(
        method = "extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;")
    )
    private <E extends Entity> EntityRenderState getAndUpdateRenderState$setEntity(EntityRenderState state, E entity, float tickProgress) {
        ((IEntityRenderState) state).meteor$setEntity(entity);
        return state;
    }
}
