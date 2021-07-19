/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Hitboxes;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Shadow public Camera camera;

    @Inject(method = "renderHitbox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawBox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/util/math/Box;FFFF)V", ordinal = 0), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void onRenderHitbox(MatrixStack matrices, VertexConsumer vertices, Entity entity, float tickDelta, CallbackInfo info, Box box) {
        double v = Modules.get().get(Hitboxes.class).getEntityValue(entity);
        if (v != 0) ((IBox) box).expand(v);
    }

    // Player model rendering in main menu

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void onRenderShadow(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity, float opacity, float tickDelta, WorldView world, float radius, CallbackInfo info) {
        if (Utils.renderingEntityOutline) info.cancel();
        if (Modules.get().get(NoRender.class).noDeadEntities() && ((LivingEntity) entity).isDead()) info.cancel();
    }

    @Inject(method = "getSquaredDistanceToCamera(Lnet/minecraft/entity/Entity;)D", at = @At("HEAD"), cancellable = true)
    private void onGetSquaredDistanceToCameraEntity(Entity entity, CallbackInfoReturnable<Double> info) {
        if (camera == null) info.setReturnValue(0.0);
    }

    @Inject(method = "getSquaredDistanceToCamera(DDD)D", at = @At("HEAD"), cancellable = true)
    private void onGetSquaredDistanceToCameraXYZ(double x, double y, double z, CallbackInfoReturnable<Double> info) {
        if (camera == null) info.setReturnValue(0.0);
    }
}
