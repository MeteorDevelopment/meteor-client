/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.block.entity.model.BannerBlockModel;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.client.render.entity.command.EntityRenderCommandQueue;
import net.minecraft.client.render.entity.command.ModelCommandRenderer;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Unit;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BannerBlockEntityRenderer.class)
public abstract class BannerBlockEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/block/entity/BannerBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;IILnet/minecraft/util/math/Vec3d;Lnet/minecraft/client/render/entity/command/ModelCommandRenderer$class_11792;Lnet/minecraft/client/render/entity/command/EntityRenderCommandQueue;)V", at = @At("HEAD"), cancellable = true)
    private void injectRender1(BannerBlockEntity bannerBlockEntity, float f, MatrixStack matrixStack, int i, int j, Vec3d vec3d, ModelCommandRenderer.class_11792 arg, EntityRenderCommandQueue entityRenderCommandQueue, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).getBannerRenderMode() == NoRender.BannerRenderMode.None) ci.cancel();
    }

    @Inject(method = "render(Lnet/minecraft/client/texture/SpriteHolder;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/entity/command/EntityRenderCommandQueue;IIFLnet/minecraft/client/render/block/entity/model/BannerBlockModel;Lnet/minecraft/client/render/block/entity/model/BannerFlagBlockModel;FLnet/minecraft/util/DyeColor;Lnet/minecraft/component/type/BannerPatternsComponent;Lnet/minecraft/client/render/entity/command/ModelCommandRenderer$class_11792;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/command/EntityRenderCommandQueue;method_73490(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/entity/command/ModelCommandRenderer$class_11792;)V"), cancellable = true)
    private static void injectRender2(SpriteHolder spriteHolder, MatrixStack matrixStack, EntityRenderCommandQueue entityRenderCommandQueue, int light, int overlay, float rotation, BannerBlockModel bannerBlockModel, BannerFlagBlockModel bannerFlagBlockModel, float g, DyeColor dyeColor, BannerPatternsComponent bannerPatternsComponent, ModelCommandRenderer.class_11792 arg, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).getBannerRenderMode() == NoRender.BannerRenderMode.Pillar) {
            renderPillar(matrixStack, entityRenderCommandQueue, light, overlay, rotation, bannerBlockModel, spriteHolder, arg);
            ci.cancel();
        }
    }

    @Unique
    private static void renderPillar(MatrixStack matrices, EntityRenderCommandQueue entityRenderCommandQueue, int light, int overlay, float rotation, BannerBlockModel model, SpriteHolder spriteHolder, ModelCommandRenderer.class_11792 arg) {
        matrices.push();
        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        matrices.scale(0.6666667F, -0.6666667F, -0.6666667F);
        SpriteIdentifier spriteIdentifier = ModelBaker.BANNER_BASE;
        entityRenderCommandQueue.method_73490(
            model,
            Unit.INSTANCE,
            matrices,
            spriteIdentifier.getRenderLayer(RenderLayer::getEntitySolid),
            light,
            overlay,
            -1,
            spriteHolder.getSprite(spriteIdentifier),
            0,
            arg
        );
        matrices.pop();
    }
}
