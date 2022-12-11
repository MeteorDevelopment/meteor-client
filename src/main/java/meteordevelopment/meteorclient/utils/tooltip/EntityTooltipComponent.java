/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.tooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

import static meteordevelopment.meteorclient.MeteorClient.mc;

// Thanks to
// https://github.com/Queerbric/Inspecio/blob/1.17/src/main/java/io/github/queerbric/inspecio/tooltip/EntityTooltipComponent.java
public class EntityTooltipComponent implements MeteorTooltipData, TooltipComponent {
    protected final Entity entity;

    public EntityTooltipComponent(Entity entity) {
        this.entity = entity;
    }

    @Override
    public TooltipComponent getComponent() {
        return this;
    }

    @Override
    public int getHeight() {
        return 24;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 60;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, MatrixStack matrices, ItemRenderer itemRenderer, int z) {
        matrices.push();
        matrices.translate(15, 2, z);
        this.entity.setVelocity(1.f, 1.f, 1.f);
        this.renderEntity(matrices, x, y);
        matrices.pop();
    }

    protected void renderEntity(MatrixStack matrices, int x, int y) {
        if (mc.player == null) return;
        float size = 24;
        if (Math.max(entity.getWidth(), entity.getHeight()) > 1.0) {
            size /= Math.max(entity.getWidth(), entity.getHeight());
        }
        DiffuseLighting.disableGuiDepthLighting();
        matrices.push();
        int yOffset = 16;

        if (entity instanceof SquidEntity) {
            size = 16;
            yOffset = 2;
        }

        matrices.translate(x + 10, y + yOffset, 1050);
        matrices.scale(1f, 1f, -1);
        matrices.translate(0, 0, 1000);
        matrices.scale(size, size, size);
        Quaternionf quaternion = RotationAxis.POSITIVE_Z.rotationDegrees(180.f);
        Quaternionf quaternion2 = RotationAxis.POSITIVE_X.rotationDegrees(-10.f);
        hamiltonProduct(quaternion, quaternion2);
        matrices.multiply(quaternion);
        setupAngles();
        EntityRenderDispatcher entityRenderDispatcher = mc.getEntityRenderDispatcher();
        quaternion2.conjugate();
        entityRenderDispatcher.setRotation(quaternion2);
        entityRenderDispatcher.setRenderShadows(false);
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        entity.age = mc.player.age;
        entity.setCustomNameVisible(false);
        entityRenderDispatcher.render(entity, 0, 0, 0, 0.f, 1.f, matrices, immediate, 15728880);
        immediate.draw();
        entityRenderDispatcher.setRenderShadows(true);
        matrices.pop();
        DiffuseLighting.enableGuiDepthLighting();
    }

    public void hamiltonProduct(Quaternionf q, Quaternionf other) {
        float f = q.x();
        float g = q.y();
        float h = q.z();
        float i = q.w();
        float j = other.x();
        float k = other.y();
        float l = other.z();
        float m = other.w();
        q.x = (((i * j) + (f * m)) + (g * l)) - (h * k);
        q.y = ((i * k) - (f * l)) + (g * m) + (h * j);
        q.z = (((i * l) + (f * k)) - (g * j)) + (h * m);
        q.w = (((i * m) - (f * j)) - (g * k)) - (h * l);
    }

    protected void setupAngles() {
        float yaw = (float) (((System.currentTimeMillis() / 10)) % 360);
        entity.setYaw(yaw);
        entity.setHeadYaw(yaw);
        entity.setPitch(0.f);
        if (entity instanceof LivingEntity) {
            if (entity instanceof GoatEntity) ((LivingEntity) entity).headYaw = yaw;
            ((LivingEntity) entity).bodyYaw = yaw;
        }
    }
}
