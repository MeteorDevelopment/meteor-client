/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.ApplyTransformationEvent;
import meteordevelopment.meteorclient.events.render.RenderItemEntityEvent;
import meteordevelopment.meteorclient.mixin.LayerRenderStateAccessor;
import meteordevelopment.meteorclient.mixininterface.IBakedQuad;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.List;

public class ItemPhysics extends Module {
    private static final Direction[] FACES = { null, Direction.UP, Direction.DOWN, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST };
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

        if (event.renderState.itemRenderState.isEmpty())
            return;

        MatrixStack matrices = event.matrixStack;

        random.setSeed(event.renderState.seed);

        for (int i = 0; i < event.renderState.itemRenderState.layerCount; i++) {
            ItemRenderState.LayerRenderState layer = event.renderState.itemRenderState.layers[i];
            ModelInfo info = getInfo(layer.getQuads());

            matrices.push();
            applyTransformation(matrices, ((LayerRenderStateAccessor) layer).getTransform());
            matrices.translate(0, info.offsetY, 0);
            offsetInWater(matrices, event.itemEntity);

            if (info.flat) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                matrices.translate(0, 0, info.offsetZ);
            }

            if (randomRotation.get()) {
                RotationAxis axis = RotationAxis.POSITIVE_Y;
                if (info.flat) axis = RotationAxis.POSITIVE_Z;

                float degrees = (random.nextFloat() * 2 - 1) * 90;
                matrices.multiply(axis.rotationDegrees(degrees));
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

            event.renderState.itemRenderState.render(matrices, event.vertexConsumerProvider, event.light, OverlayTexture.DEFAULT_UV);

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

        for (BakedQuad _quad : quads) {
            IBakedQuad quad = (IBakedQuad) (Object) _quad;

            for (int i = 0; i < 4; i++) {
                switch (_quad.face()) {
                    case DOWN -> minY = Math.min(minY, quad.meteor$getY(i));
                    case UP -> maxY = Math.max(maxY, quad.meteor$getY(i));
                    case NORTH -> minZ = Math.min(minZ, quad.meteor$getZ(i));
                    case SOUTH -> maxZ = Math.max(maxZ, quad.meteor$getZ(i));
                    case WEST -> minX = Math.min(minX, quad.meteor$getX(i));
                    case EAST -> maxX = Math.max(maxX, quad.meteor$getX(i));
                }
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

        return new ModelInfo(flat, 0.5f - minY, minZ - minY);
    }

    record ModelInfo(boolean flat, float offsetY, float offsetZ) {}
}
