/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.Chams;
import minegame159.meteorclient.modules.render.NoRender;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void onRenderHead(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo info) {
        NoRender noRender = ModuleManager.INSTANCE.get(NoRender.class);

        if ((noRender.noItems() && entity instanceof ItemEntity) ||
                (noRender.noFallingBlocks() && entity instanceof FallingBlockEntity) ||
                (noRender.noArmorStands() && entity instanceof ArmorStandEntity)
        ) {
            info.cancel();
            return;
        }

        if (ModuleManager.INSTANCE.get(Chams.class).shouldRender(entity)) {
            GL11.glEnable(32823);
            GL11.glPolygonOffset(1.0f, -1000000.0f);
        }
    }

    @Inject(method = "render", at = @At("TAIL"), cancellable = true)
    private <E extends Entity> void onRenderTail(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(Chams.class).shouldRender(entity)) {
            GL11.glPolygonOffset(1.0f, 1000000.0f);
            GL11.glDisable(32823);
        }
    }
}
