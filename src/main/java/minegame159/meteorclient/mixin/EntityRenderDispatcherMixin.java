/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IBox;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.combat.Hitboxes;
import minegame159.meteorclient.modules.render.Chams;
import minegame159.meteorclient.modules.render.NoRender;
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
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void onRenderHead(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo info) {
        NoRender noRender = Modules.get().get(NoRender.class);

        if ((noRender.noItems() && entity instanceof ItemEntity) ||
                (noRender.noFallingBlocks() && entity instanceof FallingBlockEntity) ||
                (noRender.noArmorStands() && entity instanceof ArmorStandEntity) || ( noRender.noXpOrbs() && entity instanceof ExperienceOrbEntity)
        ) {
            info.cancel();
            return;
        }

        Chams chams = Modules.get().get(Chams.class);

        if (chams.ignoreRender(entity) || !chams.isActive()) return;

        if (chams.throughWalls.get()) {
//            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
//            GL11.glPolygonOffset(1.0f, -1000000.0f);
            GL11.glDepthRange(0.0, 0.01);
        }
    }

    @Inject(method = "render", at = @At("TAIL"), cancellable = true)
    private <E extends Entity> void onRenderTail(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo info) {

        Chams chams = Modules.get().get(Chams.class);

        if (chams.ignoreRender(entity) || !chams.isActive()) return;

        if (chams.throughWalls.get()) {
//            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
//            GL11.glPolygonOffset(1.0f, -1000000.0f);
            GL11.glDepthRange(0.0, 1.0);
        }
    }

    @Inject(method = "drawBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawBox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/util/math/Box;FFFF)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onDrawBox(MatrixStack matrix, VertexConsumer vertices, Entity entity, float red, float green, float blue, CallbackInfo info, Box box) {
        double v = Modules.get().get(Hitboxes.class).getEntityValue(entity);
        if (v != 0) ((IBox) box).expand(v);
    }
}
