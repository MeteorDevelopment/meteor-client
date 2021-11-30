/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.RenderItemEntityEvent;
import meteordevelopment.meteorclient.mixininterface.IItemEntity;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.shape.VoxelShape;

public class ItemPhysics extends Module {
    public ItemPhysics() {
        super(Categories.Render, "item-physics", "Applies physics to items on the ground.");
    }

    @EventHandler
    private void onRenderItemEntity(RenderItemEntityEvent event) {
        ItemStack itemStack = event.itemEntity.getStack();
        int seed = itemStack.isEmpty() ? 187 : Item.getRawId(itemStack.getItem()) + itemStack.getDamage();
        event.random.setSeed(seed);

        event.matrixStack.push();

        BakedModel bakedModel = event.itemRenderer.getModel(itemStack, event.itemEntity.world, null, 0);
        boolean hasDepthInGui = bakedModel.hasDepth();
        int renderCount = getRenderedAmount(itemStack);
        IItemEntity rotator = (IItemEntity) event.itemEntity;
        boolean renderBlockFlat = false;

        if (event.itemEntity.getStack().getItem() instanceof BlockItem && !(event.itemEntity.getStack().getItem() instanceof AliasedBlockItem)) {
            Block b = ((BlockItem) event.itemEntity.getStack().getItem()).getBlock();
            VoxelShape shape = b.getOutlineShape(b.getDefaultState(), event.itemEntity.world, event.itemEntity.getBlockPos(), ShapeContext.absent());

            if (shape.getMax(Direction.Axis.Y) <= .5) renderBlockFlat = true;
        }

        Item item = event.itemEntity.getStack().getItem();
        if (item instanceof BlockItem && !(item instanceof AliasedBlockItem) && !renderBlockFlat) {
            event.matrixStack.translate(0, -0.06, 0);
        }

        if (!renderBlockFlat) {
            event.matrixStack.translate(0, .185, .0);
            event.matrixStack.multiply(Vec3f.POSITIVE_X.getRadialQuaternion(1.571F));
            event.matrixStack.translate(0, -.185, -.0);
        }

        boolean isAboveWater = event.itemEntity.world.getBlockState(event.itemEntity.getBlockPos()).getFluidState().getFluid().isIn(FluidTags.WATER);
        if (!event.itemEntity.isOnGround() && (!event.itemEntity.isSubmergedInWater() && !isAboveWater)) {
            float rotation = ((float) event.itemEntity.getItemAge() + event.tickDelta) / 20.0F + event.itemEntity.uniqueOffset; // calculate rotation based on age and ticks

            if (!renderBlockFlat) {
                event.matrixStack.translate(0, .185, .0);
                event.matrixStack.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion(rotation));
                event.matrixStack.translate(0, -.185, .0);
                rotator.setRotation(new Vec3d(0, 0, rotation));
            } else {
                event.matrixStack.multiply(Vec3f.POSITIVE_Y.getRadialQuaternion(rotation));
                rotator.setRotation(new Vec3d(0, rotation, 0));
                event.matrixStack.translate(0, -.065, 0);
            }

            if (event.itemEntity.getStack().getItem() instanceof AliasedBlockItem) {
                event.matrixStack.translate(0, 0, .195);
            } else if (!(event.itemEntity.getStack().getItem() instanceof BlockItem)) {
                event.matrixStack.translate(0, 0, .195);
            }
        } else if (event.itemEntity.getStack().getItem() instanceof AliasedBlockItem) {
            event.matrixStack.translate(0, .185, .0);
            event.matrixStack.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion((float) rotator.getRotation().z));
            event.matrixStack.translate(0, -.185, .0);
            event.matrixStack.translate(0, 0, .195);
        } else if (renderBlockFlat) {
            event.matrixStack.multiply(Vec3f.POSITIVE_Y.getRadialQuaternion((float) rotator.getRotation().y));
            event.matrixStack.translate(0, -.065, 0);
        } else {
            if (!(event.itemEntity.getStack().getItem() instanceof BlockItem)) {
                event.matrixStack.translate(0, 0, .195);
            }

            event.matrixStack.translate(0, .185, .0);
            event.matrixStack.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion((float) rotator.getRotation().z));
            event.matrixStack.translate(0, -.185, .0);
        }

        if (event.itemEntity.world.getBlockState(event.itemEntity.getBlockPos()).getBlock().equals(Blocks.SOUL_SAND)) {
            event.matrixStack.translate(0, 0, -.1);
        }

        if (event.itemEntity.getStack().getItem() instanceof BlockItem) {
            if (((BlockItem) event.itemEntity.getStack().getItem()).getBlock() instanceof SkullBlock) {
                event.matrixStack.translate(0, .11, 0);
            }
        }

        float scaleX = bakedModel.getTransformation().ground.scale.getX();
        float scaleY = bakedModel.getTransformation().ground.scale.getY();
        float scaleZ = bakedModel.getTransformation().ground.scale.getZ();

        float x;
        float y;
        if (!hasDepthInGui) {
            float r = -0.0F * (float) (renderCount) * 0.5F * scaleX;
            x = -0.0F * (float) (renderCount) * 0.5F * scaleY;
            y = -0.09375F * (float) (renderCount) * 0.5F * scaleZ;
            event.matrixStack.translate(r, x, y);
        }

        for (int u = 0; u < renderCount; ++u) {
            event.matrixStack.push();
            if (u > 0) {
                if (hasDepthInGui) {
                    x = (event.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    y = (event.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float z = (event.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    event.matrixStack.translate(x, y, z);
                } else {
                    x = (event.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    y = (event.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    event.matrixStack.translate(x, y, 0.0D);
                    event.matrixStack.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion(event.random.nextFloat()));
                }
            }

            event.itemRenderer.renderItem(itemStack, ModelTransformation.Mode.GROUND, false, event.matrixStack, event.vertexConsumerProvider, event.light, OverlayTexture.DEFAULT_UV, bakedModel);

            event.matrixStack.pop();

            if (!hasDepthInGui) {
                event.matrixStack.translate(0.0F * scaleX, 0.0F * scaleY, 0.0625F * scaleZ);
            }
        }

        event.matrixStack.pop();
        event.setCancelled(true);
    }

    private int getRenderedAmount(ItemStack stack) {
        int i = 1;

        if (stack.getCount() > 48) i = 5;
        else if (stack.getCount() > 32) i = 4;
        else if (stack.getCount() > 16) i = 3;
        else if (stack.getCount() > 1) i = 2;

        return i;
    }
}
