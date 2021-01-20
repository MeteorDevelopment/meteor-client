/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.render.RenderItemEntityEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Random;

public class ItemPhysics extends Module {

    public ItemPhysics() {
        super(Category.Render, "item-physics", "Applies physics to ground items.");
    }

    @EventHandler
    private final Listener<RenderItemEntityEvent> onRenderItemEntity = new Listener<>(event -> {
        event.setCancelled(true);

        ItemStack itemStack = event.itemEntity.getStack();
        Random random = new Random(itemStack.isEmpty() ? 187 : Item.getRawId(itemStack.getItem()) + itemStack.getDamage());
        float scaleX;
        float scaleY;
        float scaleZ;
        double x;
        double y;
        double z;

        event.matrixStack.push();
        BakedModel bakedModel = mc.getItemRenderer().getHeldItemModel(itemStack, mc.world, null);

        boolean hasDepthInGui = bakedModel.hasDepth();
        int renderCount = getRenderedAmount(itemStack);

        event.matrixStack.multiply(Vector3f.POSITIVE_X.getRadialQuaternion(1.571F));

        if (!event.itemEntity.isOnGround() && !event.itemEntity.isSubmergedInWater()) {
            scaleX = ((float)event.itemEntity.getAge() + event.tickDelta) / 20.0F + event.itemEntity.hoverHeight;
            event.matrixStack.multiply(Vector3f.POSITIVE_Z.getRadialQuaternion(scaleX));
        }

        event.matrixStack.translate(0, 0, -0.01);

        if (event.itemEntity.getStack().getItem() instanceof BlockItem) event.matrixStack.translate(0.0D, 0.0D, -0.12D);

        scaleX = bakedModel.getTransformation().ground.scale.getX();
        scaleY = bakedModel.getTransformation().ground.scale.getY();
        scaleZ = bakedModel.getTransformation().ground.scale.getZ();

        if (!hasDepthInGui) {
            x = (double)(-0.0F * (float)renderCount) * 0.5D * (double)scaleX;
            y = (double)(-0.0F * (float)renderCount) * 0.5D * (double)scaleY;
            z = (double)(-0.09375F * (float)renderCount) * 0.5D * (double)scaleZ;
            event.matrixStack.translate(x, y, z);
        }

        for(int u = 0; u < renderCount; ++u) {
            event.matrixStack.push();
            if (u > 0) {
                x = ((double) random.nextFloat() * 2.0D - 1.0D) * 0.15D * 0.5D;
                y = ((double) random.nextFloat() * 2.0D - 1.0D) * 0.15D * 0.5D;
                if (hasDepthInGui) {
                    z = ((double)(random.nextFloat() * 20.0F) - 1.0D) * 0.15D;
                    event.matrixStack.translate(x, y, z);
                } else {
                    event.matrixStack.translate(x, y, 0.0D);
                    event.matrixStack.multiply(Vector3f.POSITIVE_Z.getRadialQuaternion(random.nextFloat()));
                }
            }

            mc.getItemRenderer().renderItem(itemStack, ModelTransformation.Mode.GROUND, false, event.matrixStack, event.vertexConsumerProvider, event.light, OverlayTexture.DEFAULT_UV, bakedModel);
            event.matrixStack.pop();

            if (!hasDepthInGui) event.matrixStack.translate(0.0D * (double)scaleX, 0.0D * (double)scaleY, 0.0625D * (double)scaleZ);
        }

        event.matrixStack.pop();
    });

    private int getRenderedAmount(ItemStack stack) {
        int i = 1;

        if (stack.getCount() > 48) i = 5;
        else if (stack.getCount() > 32) i = 4;
        else if (stack.getCount() > 16) i = 3;
        else if (stack.getCount() > 1) i = 2;

        return i;
    }
}
