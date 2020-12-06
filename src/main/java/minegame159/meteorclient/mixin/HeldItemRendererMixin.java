/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.HandView;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = {"renderFirstPersonItem"}, at = @At(value = "TAIL", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void sex(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        HandView module = ModuleManager.INSTANCE.get(HandView.class);
        if (!module.isActive()) return;
        GlStateManager.scaled(module.scaleX(), module.scaleY(), module.scaleZ());
        GlStateManager.translated(module.posX(), module.posY(), module.posZ());
        GlStateManager.rotatef((module.rotationY() * 360.0f), 1.0f, 0.0f, 0.0f);
        GlStateManager.rotatef(-(module.rotationX() * 360.0f), 0.0f, 1.0f, 0.0f);
        GlStateManager.rotatef((module.rotationZ() * 360.0f), 0.0f, 0.0f, 1.0f);
    }
}
