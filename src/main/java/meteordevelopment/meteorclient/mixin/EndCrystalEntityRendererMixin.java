/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
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
    @Inject(method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void render(EndCrystalEntity endCrystalEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        Chams module = Modules.get().get(Chams.class);

        END_CRYSTAL = RenderLayer.getEntityTranslucent((module.isActive() && module.crystals.get() && !module.crystalsTexture.get()) ? Chams.BLANK : TEXTURE);
    }

    // Scale
    @ModifyArgs(method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V", ordinal = 0))
    private void modifyScale(Args args) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.crystals.get()) return;

        args.set(0, 2.0F * module.crystalsScale.get().floatValue());
        args.set(1, 2.0F * module.crystalsScale.get().floatValue());
        args.set(2, 2.0F * module.crystalsScale.get().floatValue());
    }

    // Bounce
    @Redirect(method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EndCrystalEntityRenderer;getYOffset(Lnet/minecraft/entity/decoration/EndCrystalEntity;F)F"))
    private float getYOff(EndCrystalEntity crystal, float tickDelta) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.crystals.get()) return getYOffset(crystal, tickDelta);

        float f = (float) crystal.endCrystalAge + tickDelta;
        float g = MathHelper.sin(f * 0.2F) / 2.0F + 0.5F;
        g = (g * g + g) * 0.4F * module.crystalsBounce.get().floatValue();
        return g - 1.4F;
    }

    // Rotation speed
    @ModifyArgs(method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3f;getDegreesQuaternion(F)Lnet/minecraft/util/math/Quaternion;"))
    private void modifySpeed(Args args) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.crystals.get()) return;

        args.set(0, ((float) args.get(0)) * module.crystalsRotationSpeed.get().floatValue());
    }

    // Core
    @Redirect(method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 3))
    private void modifyCore(ModelPart modelPart, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.crystals.get()) {
            core.render(matrices, vertices, light, overlay);
            return;
        }

        if (module.renderCore.get()) {
            Color color = module.crystalsCoreColor.get();
            core.render(matrices, vertices, light, overlay, color.r/255f, color.g/255f, color.b/255f, color.a/255f);
        }
    }

    // Frame
    @Redirect(method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 1))
    private void modifyFrame1(ModelPart modelPart, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.crystals.get()) {
            frame.render(matrices, vertices, light, overlay);
            return;
        }

        if (module.renderFrame1.get()) {
            Color color = module.crystalsFrame1Color.get();
            frame.render(matrices, vertices, light, overlay, color.r/255f, color.g/255f, color.b/255f, color.a/255f);
        }
    }

    @Redirect(method = "render(Lnet/minecraft/entity/decoration/EndCrystalEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V", ordinal = 2))
    private void modifyFrame2(ModelPart modelPart, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        Chams module = Modules.get().get(Chams.class);
        if (!module.isActive() || !module.crystals.get()) {
            frame.render(matrices, vertices, light, overlay);
            return;
        }

        if (module.renderFrame2.get()) {
            Color color = module.crystalsFrame2Color.get();
            frame.render(matrices, vertices, light, overlay, color.r/255f, color.g/255f, color.b/255f, color.a/255f);
        }
    }
}
