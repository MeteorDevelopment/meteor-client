/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IBox;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.combat.Hitboxes;
import minegame159.meteorclient.modules.render.NoRender;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Box;
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

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void onRenderHead(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo info) {
        NoRender noRender = Modules.get().get(NoRender.class);
        if ((noRender.noItems() && entity instanceof ItemEntity)
                || (noRender.noFallingBlocks() && entity instanceof FallingBlockEntity)
                || (noRender.noArmorStands() && entity instanceof ArmorStandEntity)
                || ( noRender.noXpOrbs() && entity instanceof ExperienceOrbEntity)
        ) info.cancel();
    }

    @Inject(method = "drawBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawBox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/util/math/Box;FFFF)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onDrawBox(MatrixStack matrix, VertexConsumer vertices, Entity entity, float red, float green, float blue, CallbackInfo info, Box box) {
        double v = Modules.get().get(Hitboxes.class).getEntityValue(entity);
        if (v != 0) ((IBox) box).expand(v);
    }

    // Player model rendering in main menu

    @Inject(method = "getSquaredDistanceToCamera(Lnet/minecraft/entity/Entity;)D", at = @At("HEAD"), cancellable = true)
    private void onGetSquaredDistanceToCameraEntity(Entity entity, CallbackInfoReturnable<Double> info) {
        if (camera == null) info.setReturnValue(0.0);
    }

    @Inject(method = "getSquaredDistanceToCamera(DDD)D", at = @At("HEAD"), cancellable = true)
    private void onGetSquaredDistanceToCameraXYZ(double x, double y, double z, CallbackInfoReturnable<Double> info) {
        if (camera == null) info.setReturnValue(0.0);
    }
}
