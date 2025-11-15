/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderManager.class)
public abstract class EntityRenderManagerMixin {
    @Shadow
    public Camera camera;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <S extends EntityRenderState> void render(S renderState, CameraRenderState cameraRenderState, double d, double e, double f, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CallbackInfo info) {
        var entity = ((IEntityRenderState) renderState).meteor$getEntity();

        if (entity instanceof FakePlayerEntity player && player.hideWhenInsideCamera) {
            int cX = MathHelper.floor(this.camera.getPos().x);
            int cY = MathHelper.floor(this.camera.getPos().y);
            int cZ = MathHelper.floor(this.camera.getPos().z);

            if (cX == entity.getBlockX() && cZ == entity.getBlockZ() && (cY == entity.getBlockY() || cY == entity.getBlockY() + 1)) info.cancel();
        }
    }

    // IEntityRenderState

    @ModifyExpressionValue(
        method = "getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderer;getAndUpdateRenderState(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/entity/state/EntityRenderState;")
    )
    private <E extends Entity> EntityRenderState getAndUpdateRenderState$setEntity(EntityRenderState state, E entity, float tickProgress) {
        ((IEntityRenderState) state).meteor$setEntity(entity);
        return state;
    }
}
