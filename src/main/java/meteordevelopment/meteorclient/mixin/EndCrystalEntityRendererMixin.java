/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static net.minecraft.client.render.entity.EndCrystalEntityRenderer.getYOffset;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class EndCrystalEntityRendererMixin {
    @Mutable @Shadow @Final
    private static RenderLayer END_CRYSTAL;

    @Shadow @Final
    private static Identifier TEXTURE;

    @Shadow @Final
    public ModelPart core;

    @Shadow @Final
    public ModelPart frame;

    // Texture
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void render(EndCrystalEntityRenderState endCrystalEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        Chams module = Modules.get().get(Chams.class);

        END_CRYSTAL = RenderLayer.getEntityTranslucent((module.isActive() && module.crystals.get() && !module.crystalsTexture.get()) ? Chams.BLANK : TEXTURE);
    }

    // Scale
    @ModifyArgs(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V", ordinal = 0))
    private void modifyScale(Args args) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.crystals.get()) return;

        args.set(0, 2.0F * module.crystalsScale.get().floatValue());
        args.set(1, 2.0F * module.crystalsScale.get().floatValue());
        args.set(2, 2.0F * module.crystalsScale.get().floatValue());
    }

    // Bounce
    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EndCrystalEntityRenderer;getYOffset(F)F"))
    private float getYOff(float age) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.crystals.get()) return getYOffset(age);

        float g = MathHelper.sin(age * 0.2F) / 2.0F + 0.5F;
        g = (g * g + g) * 0.4F * module.crystalsBounce.get().floatValue();
        return g - 1.4F;
    }


    // Core
    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EndCrystalEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 3))
    private void modifyCore(ModelPart modelPart, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.crystals.get()) {
            core.render(matrices, vertices, light, overlay);
            return;
        }

        if (module.renderCore.get()) {
            Color color = module.crystalsCoreColor.get();
            core.render(matrices, vertices, light, overlay, color.getPacked());
        }
    }

    // Frame
    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EndCrystalEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 1))
    private void modifyFrame1(ModelPart modelPart, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.crystals.get()) {
            frame.render(matrices, vertices, light, overlay);
            return;
        }

        if (module.renderFrame1.get()) {
            Color color = module.crystalsFrame1Color.get();
            frame.render(matrices, vertices, light, overlay, color.getPacked());
        }
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EndCrystalEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 2))
    private void modifyFrame2(ModelPart modelPart, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.crystals.get()) {
            frame.render(matrices, vertices, light, overlay);
            return;
        }

        if (module.renderFrame2.get()) {
            Color color = module.crystalsFrame2Color.get();
            frame.render(matrices, vertices, light, overlay, color.getPacked());
        }
    }
}
