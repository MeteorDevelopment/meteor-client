/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BannerBlockEntityRenderer.class)
public abstract class BannerBlockEntityRendererMixin {

    @Final
    @Shadow private ModelPart pillar;
    @Final
    @Shadow private ModelPart crossbar;

    @Inject(method = "render(Lnet/minecraft/block/entity/BannerBlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V", at = @At("HEAD"), cancellable = true)
    private void render(BannerBlockEntity bannerBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j, CallbackInfo ci) {
        if (bannerBlockEntity.getWorld() != null) { //Don't modify banners in item form
            NoRender.BannerRenderMode renderMode = Modules.get().get(NoRender.class).getBannerRenderMode();
            if (renderMode == NoRender.BannerRenderMode.None) ci.cancel();
            else if (renderMode == NoRender.BannerRenderMode.Pillar) {
                BlockState blockState = bannerBlockEntity.getCachedState();
                if (blockState.getBlock() instanceof BannerBlock) { //Floor banner
                    this.pillar.visible = true;
                    this.crossbar.visible = false;
                    renderPillar(bannerBlockEntity, matrixStack, vertexConsumerProvider, i, j);
                }
                else { //Wall banner
                    this.pillar.visible = false;
                    this.crossbar.visible = true;
                    renderCrossbar(bannerBlockEntity, matrixStack, vertexConsumerProvider, i, j);
                }
                ci.cancel();
            }
        }
    }

    @Unique
    private void renderPillar(BannerBlockEntity bannerBlockEntity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j) {
        matrixStack.push();
        BlockState blockState = bannerBlockEntity.getCachedState();
        matrixStack.translate(0.5D, 0.5D, 0.5D);
        float h = (-(Integer)blockState.get(BannerBlock.ROTATION) * 360) / 16.0F;
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(h));
        matrixStack.push();
        matrixStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
        VertexConsumer vertexConsumer = ModelLoader.BANNER_BASE.getVertexConsumer(vertexConsumerProvider, RenderLayer::getEntitySolid);
        this.pillar.render(matrixStack, vertexConsumer, i, j);
        matrixStack.pop();
        matrixStack.pop();
    }

    @Unique
    private void renderCrossbar(BannerBlockEntity bannerBlockEntity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j) {
        matrixStack.push();
        BlockState blockState = bannerBlockEntity.getCachedState();
        matrixStack.translate(0.5D, -0.1666666716337204D, 0.5D);
        float h = -blockState.get(WallBannerBlock.FACING).asRotation();
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(h));
        matrixStack.translate(0.0D, -0.3125D, -0.4375D);
        matrixStack.push();
        matrixStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
        VertexConsumer vertexConsumer = ModelLoader.BANNER_BASE.getVertexConsumer(vertexConsumerProvider, RenderLayer::getEntitySolid);
        this.crossbar.render(matrixStack, vertexConsumer, i, j);
        matrixStack.pop();
        matrixStack.pop();
    }

}
