/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WireframeEntityRenderer {
    private static final MatrixStack matrices = new MatrixStack();
    private static final Vector4f pos1 = new Vector4f();
    private static final Vector4f pos2 = new Vector4f();
    private static final Vector4f pos3 = new Vector4f();
    private static final Vector4f pos4 = new Vector4f();

    private static double offsetX, offsetY, offsetZ;
    private static Color sideColor, lineColor;
    private static ShapeMode shapeMode;

    public static void render(Render3DEvent event, Entity entity, double scale, Color sideColor, Color lineColor, ShapeMode shapeMode) {
        WireframeEntityRenderer.sideColor = sideColor;
        WireframeEntityRenderer.lineColor = lineColor;
        WireframeEntityRenderer.shapeMode = shapeMode;

        offsetX = MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX());
        offsetY = MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY());
        offsetZ = MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ());

        matrices.push();
        matrices.scale((float) scale, (float) scale, (float) scale);

        EntityRenderer<?> entityRenderer = mc.getEntityRenderDispatcher().getRenderer(entity);

        // LivingEntityRenderer
        if (entityRenderer instanceof LivingEntityRenderer renderer) {
            LivingEntity livingEntity = (LivingEntity) entity;
            EntityModel<LivingEntity> model = renderer.getModel();

            // PlayerEntityRenderer
            if (entityRenderer instanceof PlayerEntityRenderer r) {
                PlayerEntityModel<AbstractClientPlayerEntity> playerModel = r.getModel();

                playerModel.sneaking = entity.isInSneakingPose();
                BipedEntityModel.ArmPose armPose = PlayerEntityRenderer.getArmPose((AbstractClientPlayerEntity) entity, Hand.MAIN_HAND);
                BipedEntityModel.ArmPose armPose2 = PlayerEntityRenderer.getArmPose((AbstractClientPlayerEntity) entity, Hand.OFF_HAND);

                if (armPose.isTwoHanded()) armPose2 = livingEntity.getOffHandStack().isEmpty() ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;

                if (livingEntity.getMainArm() == Arm.RIGHT) {
                    playerModel.rightArmPose = armPose;
                    playerModel.leftArmPose = armPose2;
                } else {
                    playerModel.rightArmPose = armPose2;
                    playerModel.leftArmPose = armPose;
                }
            }

            model.handSwingProgress = livingEntity.getHandSwingProgress(event.tickDelta);
            model.riding = livingEntity.hasVehicle();
            model.child = livingEntity.isBaby();

            float bodyYaw = MathHelper.lerpAngleDegrees(event.tickDelta, livingEntity.prevBodyYaw, livingEntity.bodyYaw);
            float headYaw = MathHelper.lerpAngleDegrees(event.tickDelta, livingEntity.prevHeadYaw, livingEntity.headYaw);
            float yaw = headYaw - bodyYaw;

            float animationProgress;
            if (livingEntity.hasVehicle() && livingEntity.getVehicle() instanceof LivingEntity livingEntity2) {
                bodyYaw = MathHelper.lerpAngleDegrees(event.tickDelta, livingEntity2.prevBodyYaw, livingEntity2.bodyYaw);
                yaw = headYaw - bodyYaw;
                animationProgress = MathHelper.wrapDegrees(yaw);

                if (animationProgress < -85) animationProgress = -85;
                if (animationProgress >= 85) animationProgress = 85;

                bodyYaw = headYaw - animationProgress;
                if (animationProgress * animationProgress > 2500) bodyYaw += animationProgress * 0.2;

                yaw = headYaw - bodyYaw;
            }

            float pitch = MathHelper.lerp(event.tickDelta, livingEntity.prevPitch, livingEntity.getPitch());

            animationProgress = renderer.getAnimationProgress(livingEntity, event.tickDelta);
            float limbDistance = 0;
            float limbAngle = 0;

            if (!livingEntity.hasVehicle() && livingEntity.isAlive()) {
                limbDistance = MathHelper.lerp(event.tickDelta, livingEntity.lastLimbDistance, livingEntity.limbDistance);
                limbAngle = livingEntity.limbAngle - livingEntity.limbDistance * (1 - event.tickDelta);

                if (livingEntity.isBaby()) limbAngle *= 3;
                if (limbDistance > 1) limbDistance = 1;
            }

            model.animateModel(livingEntity, limbAngle, limbDistance, event.tickDelta);
            model.setAngles(livingEntity, limbAngle, limbDistance, animationProgress, yaw, pitch);

            renderer.setupTransforms(livingEntity, matrices, animationProgress, bodyYaw, event.tickDelta);
            matrices.scale(-1, -1, 1);
            renderer.scale(livingEntity, matrices, event.tickDelta);
            matrices.translate(0, -1.5010000467300415, 0);

            // Render
            if (model instanceof AnimalModel m) {
                if (m.child) {
                    matrices.push();
                    float g;
                    if (m.headScaled) {
                        g = 1.5F / m.invertedChildHeadScale;
                        matrices.scale(g, g, g);
                    }

                    matrices.translate(0.0D, m.childHeadYOffset / 16.0F, m.childHeadZOffset / 16.0F);
                    if (model instanceof BipedEntityModel mo) render(event.renderer, mo.head);
                    else m.getHeadParts().forEach(modelPart -> render(event.renderer, (ModelPart) modelPart));
                    matrices.pop();
                    matrices.push();
                    g = 1.0F / m.invertedChildBodyScale;
                    matrices.scale(g, g, g);
                    matrices.translate(0.0D, m.childBodyYOffset / 16.0F, 0.0D);
                    if (model instanceof BipedEntityModel mo) {
                        render(event.renderer, mo.body);
                        render(event.renderer, mo.leftArm);
                        render(event.renderer, mo.rightArm);
                        render(event.renderer, mo.leftLeg);
                        render(event.renderer, mo.rightLeg);
                    }
                    else m.getBodyParts().forEach(modelPart -> render(event.renderer, (ModelPart) modelPart));
                    matrices.pop();
                }
                else {
                    if (model instanceof BipedEntityModel mo) {
                        render(event.renderer, mo.head);
                        render(event.renderer, mo.body);
                        render(event.renderer, mo.leftArm);
                        render(event.renderer, mo.rightArm);
                        render(event.renderer, mo.leftLeg);
                        render(event.renderer, mo.rightLeg);
                    }
                    else {
                        m.getHeadParts().forEach(modelPart -> render(event.renderer, (ModelPart) modelPart));
                        m.getBodyParts().forEach(modelPart -> render(event.renderer, (ModelPart) modelPart));
                    }
                }
            }
            else {
                if (model instanceof SinglePartEntityModel m) {
                    render(event.renderer, m.getPart());
                }
                else if (model instanceof CompositeEntityModel m) {
                    m.getParts().forEach(modelPart -> render(event.renderer, (ModelPart) modelPart));
                }
                else if (model instanceof LlamaEntityModel m) {
                    if (m.child) {
                        matrices.push();
                        matrices.scale(0.71428573F, 0.64935064F, 0.7936508F);
                        matrices.translate(0.0D, 1.3125D, 0.2199999988079071D);
                        render(event.renderer, m.head);
                        matrices.pop();
                        matrices.push();
                        matrices.scale(0.625F, 0.45454544F, 0.45454544F);
                        matrices.translate(0.0D, 2.0625D, 0.0D);
                        render(event.renderer, m.body);
                        matrices.pop();
                        matrices.push();
                        matrices.scale(0.45454544F, 0.41322312F, 0.45454544F);
                        matrices.translate(0.0D, 2.0625D, 0.0D);
                        render(event.renderer, m.rightHindLeg);
                        render(event.renderer, m.leftHindLeg);
                        render(event.renderer, m.rightFrontLeg);
                        render(event.renderer, m.leftFrontLeg);
                        render(event.renderer, m.rightChest);
                        render(event.renderer, m.leftChest);
                        matrices.pop();
                    }
                    else {
                        render(event.renderer, m.head);
                        render(event.renderer, m.body);
                        render(event.renderer, m.rightHindLeg);
                        render(event.renderer, m.leftHindLeg);
                        render(event.renderer, m.rightFrontLeg);
                        render(event.renderer, m.leftFrontLeg);
                        render(event.renderer, m.rightChest);
                        render(event.renderer, m.leftChest);
                    }
                }
                else if (model instanceof RabbitEntityModel m) {
                    if (m.child) {
                        matrices.push();
                        matrices.scale(0.56666666F, 0.56666666F, 0.56666666F);
                        matrices.translate(0.0D, 1.375D, 0.125D);
                        render(event.renderer, m.head);
                        render(event.renderer, m.leftEar);
                        render(event.renderer, m.rightEar);
                        render(event.renderer, m.nose);
                        matrices.pop();
                        matrices.push();
                        matrices.scale(0.4F, 0.4F, 0.4F);
                        matrices.translate(0.0D, 2.25D, 0.0D);
                        render(event.renderer, m.leftHindLeg);
                        render(event.renderer, m.rightHindLeg);
                        render(event.renderer, m.leftHaunch);
                        render(event.renderer, m.rightHaunch);
                        render(event.renderer, m.body);
                        render(event.renderer, m.leftFrontLeg);
                        render(event.renderer, m.rightFrontLeg);
                        render(event.renderer, m.tail);
                        matrices.pop();
                    }
                    else {
                        matrices.push();
                        matrices.scale(0.6F, 0.6F, 0.6F);
                        matrices.translate(0.0D, 1.0D, 0.0D);
                        render(event.renderer, m.leftHindLeg);
                        render(event.renderer, m.rightHindLeg);
                        render(event.renderer, m.leftHaunch);
                        render(event.renderer, m.rightHaunch);
                        render(event.renderer, m.body);
                        render(event.renderer, m.leftFrontLeg);
                        render(event.renderer, m.rightFrontLeg);
                        render(event.renderer, m.head);
                        render(event.renderer, m.rightEar);
                        render(event.renderer, m.leftEar);
                        render(event.renderer, m.tail);
                        render(event.renderer, m.nose);
                        matrices.pop();
                    }
                }
            }
        }

        if (entityRenderer instanceof EndCrystalEntityRenderer renderer) {
            EndCrystalEntity crystalEntity = (EndCrystalEntity) entity;
            Chams chams = Modules.get().get(Chams.class);
            boolean chamsEnabled = chams.isActive() && chams.crystals.get();

            matrices.push();
            float h;
            if (chamsEnabled) {
                float f = (float) crystalEntity.endCrystalAge + event.tickDelta;
                float g = MathHelper.sin(f * 0.2F) / 2.0F + 0.5F;
                g = (g * g + g) * 0.4F * chams.crystalsBounce.get().floatValue();
                h = g - 1.4F;
            }
            else h = EndCrystalEntityRenderer.getYOffset(crystalEntity, event.tickDelta);
            float j = ((float) crystalEntity.endCrystalAge + event.tickDelta) * 3.0F;
            matrices.push();
            if (chamsEnabled) matrices.scale(2.0F * chams.crystalsScale.get().floatValue(), 2.0F * chams.crystalsScale.get().floatValue(), 2.0F * chams.crystalsScale.get().floatValue());
            else matrices.scale(2.0F, 2.0F, 2.0F);
            matrices.translate(0.0D, -0.5D, 0.0D);
            if (crystalEntity.shouldShowBottom()) render(event.renderer, renderer.bottom);

            if (chamsEnabled) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j * chams.crystalsRotationSpeed.get().floatValue()));
            else matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
            matrices.translate(0.0D, 1.5F + h / 2.0F, 0.0D);
            matrices.multiply(new Quaternionf().setAngleAxis(60.0F, EndCrystalEntityRenderer.SINE_45_DEGREES, 0.0F, EndCrystalEntityRenderer.SINE_45_DEGREES));
            if (!chamsEnabled || chams.renderFrame1.get()) render(event.renderer, renderer.frame);
            matrices.scale(0.875F, 0.875F, 0.875F);
            matrices.multiply(new Quaternionf().setAngleAxis(60.0F, EndCrystalEntityRenderer.SINE_45_DEGREES, 0.0F, EndCrystalEntityRenderer.SINE_45_DEGREES));
            if (chamsEnabled) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j * chams.crystalsRotationSpeed.get().floatValue()));
            else matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
            if (!chamsEnabled || chams.renderFrame2.get()) render(event.renderer, renderer.frame);
            matrices.scale(0.875F, 0.875F, 0.875F);
            matrices.multiply(new Quaternionf().setAngleAxis(60.0F, EndCrystalEntityRenderer.SINE_45_DEGREES, 0.0F, EndCrystalEntityRenderer.SINE_45_DEGREES));
            if (chamsEnabled) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j * chams.crystalsRotationSpeed.get().floatValue()));
            else matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
            if (!chamsEnabled || chams.renderCore.get()) render(event.renderer, renderer.core);
            matrices.pop();
            matrices.pop();
        }
        else if (entityRenderer instanceof BoatEntityRenderer renderer) {
            BoatEntity boatEntity = (BoatEntity) entity;

            matrices.push();
            matrices.translate(0.0D, 0.375D, 0.0D);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - MathHelper.lerp(event.tickDelta, entity.prevYaw, entity.getYaw())));
            float h = (float)boatEntity.getDamageWobbleTicks() - event.tickDelta;
            float j = boatEntity.getDamageWobbleStrength() - event.tickDelta;
            if (j < 0.0F) j = 0.0F;

            if (h > 0.0F) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.sin(h) * h * j / 10.0F * (float)boatEntity.getDamageWobbleSide()));
            }

            float k = boatEntity.interpolateBubbleWobble(event.tickDelta);
            if (!MathHelper.approximatelyEquals(k, 0.0F)) {
                matrices.multiply(new Quaternionf().setAngleAxis(boatEntity.interpolateBubbleWobble(event.tickDelta), 1.0F, 0.0F, 1.0F));
            }

            CompositeEntityModel<BoatEntity> boatEntityModel = renderer.texturesAndModels.get(boatEntity.getVariant()).getSecond();
            matrices.scale(-1.0F, -1.0F, 1.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            boatEntityModel.setAngles(boatEntity, event.tickDelta, 0.0F, -0.1F, 0.0F, 0.0F);
            boatEntityModel.getParts().forEach(modelPart -> render(event.renderer, modelPart));
            if (!boatEntity.isSubmergedInWater() && boatEntityModel instanceof ModelWithWaterPatch modelWithWaterPatch) render(event.renderer, modelWithWaterPatch.getWaterPatch());

            matrices.pop();
        }
        else if (entityRenderer instanceof ItemEntityRenderer) {
            double dx = (entity.getX() - entity.prevX) * event.tickDelta;
            double dy = (entity.getY() - entity.prevY) * event.tickDelta;
            double dz = (entity.getZ() - entity.prevZ) * event.tickDelta;

            Box box = entity.getBoundingBox();
            event.renderer.box(dx + box.minX, dy + box.minY, dz + box.minZ, dx + box.maxX, dy + box.maxY, dz + box.maxZ, sideColor, lineColor, shapeMode, 0);
        }

        matrices.pop();
    }

    private static void render(Renderer3D renderer, ModelPart part) {
        if (!part.visible || (part.cuboids.isEmpty() && part.children.isEmpty())) return;

        matrices.push();
        part.rotate(matrices);

        for (ModelPart.Cuboid cuboid : part.cuboids) render(renderer, cuboid, offsetX, offsetY, offsetZ);
        for (ModelPart child : part.children.values()) render(renderer, child);

        matrices.pop();
    }

    private static void render(Renderer3D renderer, ModelPart.Cuboid cuboid, double offsetX, double offsetY, double offsetZ) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (ModelPart.Quad quad : cuboid.sides) {
            // Transform positions
            pos1.set(quad.vertices[0].pos.x / 16, quad.vertices[0].pos.y / 16, quad.vertices[0].pos.z / 16, 1);
            pos1.mul(matrix);

            pos2.set(quad.vertices[1].pos.x / 16, quad.vertices[1].pos.y / 16, quad.vertices[1].pos.z / 16, 1);
            pos2.mul(matrix);

            pos3.set(quad.vertices[2].pos.x / 16, quad.vertices[2].pos.y / 16, quad.vertices[2].pos.z / 16, 1);
            pos3.mul(matrix);

            pos4.set(quad.vertices[3].pos.x / 16, quad.vertices[3].pos.y / 16, quad.vertices[3].pos.z / 16, 1);
            pos4.mul(matrix);

            // Render
            if (shapeMode.sides()) {
                renderer.triangles.quad(
                    renderer.triangles.vec3(offsetX + pos1.x, offsetY + pos1.y, offsetZ + pos1.z).color(sideColor).next(),
                    renderer.triangles.vec3(offsetX + pos2.x, offsetY + pos2.y, offsetZ + pos2.z).color(sideColor).next(),
                    renderer.triangles.vec3(offsetX + pos3.x, offsetY + pos3.y, offsetZ + pos3.z).color(sideColor).next(),
                    renderer.triangles.vec3(offsetX + pos4.x, offsetY + pos4.y, offsetZ + pos4.z).color(sideColor).next()
                );
            }

            if (shapeMode.lines()) {
                renderer.line(offsetX + pos1.x, offsetY + pos1.y, offsetZ + pos1.z, offsetX + pos2.x, offsetY + pos2.y, offsetZ + pos2.z, lineColor);
                renderer.line(offsetX + pos2.x, offsetY + pos2.y, offsetZ + pos2.z, offsetX + pos3.x, offsetY + pos3.y, offsetZ + pos3.z, lineColor);
                renderer.line(offsetX + pos3.x, offsetY + pos3.y, offsetZ + pos3.z, offsetX + pos4.x, offsetY + pos4.y, offsetZ + pos4.z, lineColor);
                renderer.line(offsetX + pos1.x, offsetY + pos1.y, offsetZ + pos1.z, offsetX + pos1.x, offsetY + pos1.y, offsetZ + pos1.z, lineColor);
            }
        }
    }
}
