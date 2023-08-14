/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.ApplyTransformationEvent;
import meteordevelopment.meteorclient.events.render.RenderItemEntityEvent;
import meteordevelopment.meteorclient.mixininterface.IBakedQuad;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

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
    private boolean renderingItem;

    public ItemPhysics() {
        super(Categories.Render, "item-physics", "Applies physics to items on the ground.");
    }

    @EventHandler
    private void onRenderItemEntity(RenderItemEntityEvent event) {
        MatrixStack matrices = event.matrixStack;
        matrices.push();

        ItemStack itemStack = event.itemEntity.getStack();
        BakedModel model = getModel(event.itemEntity);
        ModelInfo info = getInfo(model);

        random.setSeed(event.itemEntity.getId() * 2365798L);

        applyTransformation(matrices, model);
        matrices.translate(0, info.offsetY, 0);
        offsetInWater(matrices, event.itemEntity);
        preventZFighting(matrices, event.itemEntity);

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

        renderItem(event, matrices, itemStack, model, info);

        matrices.pop();
        event.cancel();
    }

    @EventHandler
    private void onApplyTransformation(ApplyTransformationEvent event) {
        if (renderingItem) event.cancel();
    }

    private void renderItem(RenderItemEntityEvent event, MatrixStack matrices, ItemStack itemStack, BakedModel model, ModelInfo info) {
        renderingItem = true;
        int count = getRenderedCount(itemStack);

        for (int i = 0; i < count; i++) {
            matrices.push();

            if (i > 0) {
                float x = (random.nextFloat() * 2 - 1) * 0.25f;
                float z = (random.nextFloat() * 2 - 1) * 0.25f;
                translate(matrices, info, x, 0, z);
            }

            event.itemRenderer.renderItem(itemStack, ModelTransformationMode.GROUND, false, matrices, event.vertexConsumerProvider, event.light, OverlayTexture.DEFAULT_UV, model);

            matrices.pop();

            float y = Math.max(random.nextFloat() * PIXEL_SIZE, PIXEL_SIZE / 2f);
            translate(matrices, info, 0, y, 0);
        }

        renderingItem = false;
    }

    private void translate(MatrixStack matrices, ModelInfo info, float x, float y, float z) {
        if (info.flat) {
            float temp = y;
            y = z;
            z = -temp;
        }

        matrices.translate(x, y, z);
    }

    private int getRenderedCount(ItemStack stack) {
        int i = 1;

        if (stack.getCount() > 48) i = 5;
        else if (stack.getCount() > 32) i = 4;
        else if (stack.getCount() > 16) i = 3;
        else if (stack.getCount() > 1) i = 2;

        return i;
    }

    private void applyTransformation(MatrixStack matrices, BakedModel model) {
        Transformation transformation = model.getTransformation().ground;

        float prevY = transformation.translation.y;
        transformation.translation.y = 0;

        transformation.apply(false, matrices);

        transformation.translation.y = prevY;
    }

    private void offsetInWater(MatrixStack matrices, ItemEntity entity) {
        if (entity.isTouchingWater()) {
            matrices.translate(0, 0.333f, 0);
        }
    }

    private void preventZFighting(MatrixStack matrices, ItemEntity entity) {
        float offset = 0.0001f;

        float distance = (float) mc.gameRenderer.getCamera().getPos().distanceTo(entity.getPos());
        offset = Math.min(offset * Math.max(1, distance), 0.01f); // Ensure distance is at least 1 and that final offset is not bigger than 0.01

        matrices.translate(0, offset, 0);
    }

    private BakedModel getModel(ItemEntity entity) {
        ItemStack itemStack = entity.getStack();

        // Mojang be like
        if (itemStack.isOf(Items.TRIDENT)) return mc.getItemRenderer().getModels().getModelManager().getModel(ItemRenderer.TRIDENT);
        if (itemStack.isOf(Items.SPYGLASS)) return mc.getItemRenderer().getModels().getModelManager().getModel(ItemRenderer.SPYGLASS);

        return mc.getItemRenderer().getModel(itemStack, entity.getWorld(), null, entity.getId());
    }

    private ModelInfo getInfo(BakedModel model) {
        Random random = Random.createLocal();

        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = Float.MIN_VALUE;

        for (Direction face : FACES) {
            for (BakedQuad _quad : model.getQuads(null, face, random)) {
                IBakedQuad quad = (IBakedQuad) _quad;

                for (int i = 0; i < 4; i++) {
                    switch (_quad.getFace()) {
                        case DOWN -> minY = Math.min(minY, quad.meteor$getY(i));
                        case UP -> maxY = Math.max(maxY, quad.meteor$getY(i));
                        case NORTH -> minZ = Math.min(minZ, quad.meteor$getZ(i));
                        case SOUTH -> maxZ = Math.max(maxZ, quad.meteor$getZ(i));
                        case WEST -> minX = Math.min(minX, quad.meteor$getX(i));
                        case EAST -> maxX = Math.max(maxX, quad.meteor$getX(i));
                    }
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
