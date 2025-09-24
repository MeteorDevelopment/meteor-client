/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.block.entity.model.BannerBlockModel;
import net.minecraft.client.render.block.entity.model.BannerFlagBlockModel;
import net.minecraft.client.render.block.entity.state.BannerBlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Unit;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BannerBlockEntityRenderer.class)
public abstract class BannerBlockEntityRendererMixin {
    @Inject(method = "render(Lnet/minecraft/client/render/block/entity/state/BannerBlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void injectRender1(BannerBlockEntityRenderState bannerBlockEntityRenderState, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState arg, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).getBannerRenderMode() == NoRender.BannerRenderMode.None) ci.cancel();
    }

    @Inject(method = "render(Lnet/minecraft/client/texture/SpriteHolder;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIFLnet/minecraft/client/render/block/entity/model/BannerBlockModel;Lnet/minecraft/client/render/block/entity/model/BannerFlagBlockModel;FLnet/minecraft/util/DyeColor;Lnet/minecraft/component/type/BannerPatternsComponent;Lnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"), cancellable = true)
    private static void injectRender2(SpriteHolder materials, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, float yaw, BannerBlockModel model, BannerFlagBlockModel flagModel, float pitch, DyeColor dyeColor, BannerPatternsComponent bannerPatterns, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay, int outline, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).getBannerRenderMode() == NoRender.BannerRenderMode.Pillar) {
            renderPillar(matrices, queue, light, overlay, yaw, model, materials, crumblingOverlay);
            ci.cancel();
        }
    }

    @Unique
    private static void renderPillar(MatrixStack matrices, OrderedRenderCommandQueue entityRenderCommandQueue, int light, int overlay, float rotation, BannerBlockModel model, SpriteHolder spriteHolder, ModelCommandRenderer.CrumblingOverlayCommand arg) {
        matrices.push();
        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        matrices.scale(0.6666667F, -0.6666667F, -0.6666667F);
        SpriteIdentifier spriteIdentifier = ModelBaker.BANNER_BASE;
        entityRenderCommandQueue.submitModel(
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
