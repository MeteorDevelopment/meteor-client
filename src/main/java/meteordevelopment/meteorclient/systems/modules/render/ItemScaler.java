/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.RenderItemEntityEvent;
import meteordevelopment.meteorclient.mixin.ItemRenderStateAccessor;
import meteordevelopment.meteorclient.mixin.LayerRenderStateAccessor;
import meteordevelopment.meteorclient.mixininterface.IBakedQuad;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.joml.Vector3f;

import java.util.List;

public class ItemScaler extends Module {
    private static final float PIXEL_SIZE = 1f / 16f;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 缩放设置
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("物品缩放倍数")
        .defaultValue(2.0) // 默认放大2倍
        .min(0.1)
        .max(5.0)
        .sliderMax(5.0)
        .build()
    );

    // 物品列表设置
    private final Setting<List<Item>> targetItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("target-items")
        .description("要放大的物品列表")
        .defaultValue(
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.GOLD_INGOT,
            Items.DIAMOND,
            Items.EMERALD,
            Items.NETHERITE_INGOT,
            Items.PLAYER_HEAD,
            Items.CREEPER_HEAD,
            Items.ZOMBIE_HEAD,
            Items.SKELETON_SKULL,
            Items.WITHER_SKELETON_SKULL,
            Items.DRAGON_HEAD,
            Items.PIGLIN_HEAD
        )
        .build()
    );

    public ItemScaler() {
        super(Categories.Render, "item-scaler", "放大指定物品的模型大小");
    }

    @EventHandler
    private void onRenderItemEntity(RenderItemEntityEvent event) {
        // 检查物品是否在目标列表中
        Item item = event.itemEntity.getStack().getItem();
        if (!targetItems.get().contains(item)) return;

        // 应用缩放
        double s = scale.get();
        if (s == 1.0) return; // 如果是1倍缩放就跳过

        // 取消原始渲染
        event.cancel();

        // 自定义渲染逻辑
        if (event.renderState.itemRenderState.isEmpty()) return;

        MatrixStack matrices = event.matrixStack;

        // 渲染每一层
        for (int i = 0; i < ((ItemRenderStateAccessor) event.renderState.itemRenderState).meteor$getLayerCount(); i++) {
            ItemRenderState.LayerRenderState layer = ((ItemRenderStateAccessor) event.renderState.itemRenderState).meteor$getLayers()[i];
            ModelInfo info = getInfo(layer.getQuads());

            matrices.push();
            // 简化变换 - 取消随机旋转和物理效果
            matrices.translate(0, 0, 0);
            matrices.scale((float) s, (float) s, (float) s);
            
            // 应用固定的渲染位置
            translate(matrices, info, 0, 0, 0);

            renderSimpleLayer(event, info);

            matrices.pop();
        }
    }

    private void renderSimpleLayer(RenderItemEntityEvent event, ModelInfo info) {
        // 完全禁用物理效果，简化渲染
        // 取消随机堆叠效果
        // 使用固定的Y轴偏移
        translate(event.matrixStack, info, 0, 0, 0);
        
        event.renderState.itemRenderState.render(
            event.matrixStack, 
            event.renderCommandQueue, 
            event.light, 
            OverlayTexture.DEFAULT_UV, 
            event.renderState.outlineColor
        );
    }

    private void translate(MatrixStack matrices, ModelInfo info, float x, float y, float z) {
        // 完全禁用物理效果
        matrices.translate(x, 0, z);
    }

    private void applyTransformation(MatrixStack matrices, Transformation transform) {
        // 创建一个没有物理效果的转换
        // 移除所有Y轴上的物理效果
        Transformation cleanTransform = new Transformation(
            transform.rotation(),
            new Vector3f(transform.translation().x(), 0, transform.translation().z()),
            transform.scale()
        );

        cleanTransform.apply(false, matrices.peek());
    }

    private ModelInfo getInfo(List<BakedQuad> quads) {
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = Float.MIN_VALUE;

        for (BakedQuad _quad : quads) {
            IBakedQuad quad = (IBakedQuad) (Object) _quad;

            for (int i = 0; i < 4; i++) {
                minY = Math.min(minY, quad.meteor$getY(i));
                maxY = Math.max(maxY, quad.meteor$getY(i));
                minZ = Math.min(minZ, quad.meteor$getZ(i));
                maxZ = Math.max(maxZ, quad.meteor$getZ(i));
                minX = Math.min(minX, quad.meteor$getX(i));
                maxX = Math.max(maxX, quad.meteor$getX(i));
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