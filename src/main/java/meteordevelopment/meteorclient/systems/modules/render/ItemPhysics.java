/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.ApplyTransformationEvent;
import meteordevelopment.meteorclient.events.render.RenderItemEntityEvent;
import meteordevelopment.meteorclient.mixin.ItemRenderStateAccessor;
import meteordevelopment.meteorclient.mixin.LayerRenderStateAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

public class ItemPhysics extends Module {
    private static final float PIXEL_SIZE = 1f / 16f;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> randomRotation = sgGeneral.add(new BoolSetting.Builder()
        .name("random-rotation")
        .description("Adds a random rotation to every item.")
        .defaultValue(true)
        .build()
    );

    private final Random random = Random.createLocal();
    private boolean skipTransformation;

    public ItemPhysics() {
        super(Categories.Render, "item-physics", "Applies physics to items on the ground.");
    }

    @EventHandler
    private void onRenderItemEntity(RenderItemEntityEvent event) {
        event.cancel();

        if (event.renderState.itemRenderState.isEmpty() || event.itemEntity == null)
            return;

        MatrixStack matrices = event.matrixStack;

        random.setSeed(event.itemEntity.getId() * 89748956L);

        for (int i = 0; i < ((ItemRenderStateAccessor) event.renderState.itemRenderState).meteor$getLayerCount(); i++) {
            ItemRenderState.LayerRenderState layer = ((ItemRenderStateAccessor) event.renderState.itemRenderState).meteor$getLayers()[i];
            ModelInfo info = getInfo(layer.getQuads());

            matrices.push();
            applyTransformation(matrices, ((LayerRenderStateAccessor) layer).meteor$getTransform());
            matrices.translate(0, info.offsetY, 0);
            offsetInWater(matrices, event.itemEntity);

            if (info.flat) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                matrices.translate(0, 0, info.offsetZ);
            }

            if (randomRotation.get()) {
                var axis = RotationAxis.POSITIVE_Y;
                var x = 0.5f;
                var y = 0.0f;
                var z = 0.5f;

                if (info.flat) {
                    axis = RotationAxis.POSITIVE_Z;
                    y = 0.5f;
                    z = 0.0f;
                }

                float degrees = (random.nextFloat() * 2 - 1) * 90;

                matrices.translate(x, y, z);
                matrices.multiply(axis.rotationDegrees(degrees));
                matrices.translate(-x, -y, -z);
            }

            renderLayer(event, info);

            matrices.pop();
        }
    }

    @EventHandler
    private void onApplyTransformation(ApplyTransformationEvent event) {
        if (skipTransformation)
            event.cancel();
    }

    private void renderLayer(RenderItemEntityEvent event, ModelInfo info) {
        MatrixStack matrices = event.matrixStack;
        skipTransformation = true;

        for (int j = 0; j < event.renderState.renderedAmount; j++) {
            matrices.push();

            if (j > 0) {
                float x = (random.nextFloat() * 2 - 1) * 0.25f;
                float z = (random.nextFloat() * 2 - 1) * 0.25f;
                translate(matrices, info, x, 0, z);
            }

            event.renderState.itemRenderState.render(matrices, event.renderCommandQueue, event.light, OverlayTexture.DEFAULT_UV, event.renderState.outlineColor);

            matrices.pop();

            float y = Math.max(random.nextFloat() * PIXEL_SIZE, PIXEL_SIZE / 2f);
            translate(matrices, info, 0, y, 0);
        }

        skipTransformation = false;
    }

    private void translate(MatrixStack matrices, ModelInfo info, float x, float y, float z) {
        if (info.flat) {
            float temp = y;
            y = z;
            z = -temp;
        }

        matrices.translate(x, y, z);
    }

    private void applyTransformation(MatrixStack matrices, Transformation transform) {
        transform = new Transformation(
            transform.rotation(),
            new Vector3f(transform.translation().x(), 0, transform.translation().z()),
            transform.scale()
        );

        transform.apply(false, matrices.peek());
    }

    private void offsetInWater(MatrixStack matrices, ItemEntity entity) {
        if (entity.isTouchingWater()) {
            matrices.translate(0, 0.333f, 0);
        }
    }

    private ModelInfo getInfo(List<BakedQuad> quads) {
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = Float.MIN_VALUE;

        for (BakedQuad quad : quads) {
            for (int i = 0; i < 4; i++) {
                Vector3fc vec = quad.getPosition(i);
                minY = Math.min(minY, vec.y());
                maxY = Math.max(maxY, vec.y());
                minZ = Math.min(minZ, vec.z());
                maxZ = Math.max(maxZ, vec.z());
                minX = Math.min(minX, vec.x());
                maxX = Math.max(maxX, vec.x());
            }
        }

        if (minX == Float.MAX_VALUE) minX = 0;
        if (minY == Float.MAX_VALUE) minY = 0;
        if (minZ == Float.MAX_VALUE) minZ = 0;

        if (maxX == Float.MIN_VALUE) maxX = 1;
        if (maxY == Float.MIN_VALUE) maxY = 1;
        if (maxZ == Float.MIN_VALUE) maxZ = 1;

        float x = maxX - minX;
        float y = maxY - minY;
        float z = maxZ - minZ;

        boolean flat = (x > PIXEL_SIZE && y > PIXEL_SIZE && z <= PIXEL_SIZE);

        return new ModelInfo(flat, 0.5f - minY, -maxZ);
    }

    record ModelInfo(boolean flat, float offsetY, float offsetZ) {}
}
