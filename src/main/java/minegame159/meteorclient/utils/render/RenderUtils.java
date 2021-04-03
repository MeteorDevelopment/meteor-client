/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.render.FreeRotate;
import minegame159.meteorclient.systems.modules.render.Freecam;
import minegame159.meteorclient.utils.entity.Target;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class RenderUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    //Items
    public static void drawItem(ItemStack itemStack, int x, int y, boolean overlay) {
        RenderSystem.disableLighting();
        RenderSystem.disableDepthTest();
        DiffuseLighting.enable();
        mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);
        if (overlay) mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y, null);
        DiffuseLighting.disable();
        DiffuseLighting.disable();
        RenderSystem.enableDepthTest();
    }

    public static void drawItem(ItemStack itemStack, int x, int y, double scale, boolean overlay) {
        RenderSystem.pushMatrix();
        RenderSystem.scaled(scale, scale, 1);
        drawItem(itemStack, x, y, overlay);
        RenderSystem.popMatrix();
    }

    //Tracers
    public static Vec3d getCameraVector() {
        boolean dist = Modules.get().isActive(Freecam.class) || Modules.get().get(FreeRotate.class).playerMode();
        return new Vec3d(0, 0, dist ? 1 : 75)
                .rotateX(-(float) Math.toRadians(mc.gameRenderer.getCamera().getPitch()))
                .rotateY(-(float) Math.toRadians(mc.gameRenderer.getCamera().getYaw()))
                .add(mc.gameRenderer.getCamera().getPos());
    }

    public static void drawTracerToEntity(RenderEvent event, Entity entity, Color color, Target target, boolean stem) {
        double x = entity.prevX + (entity.getX() - entity.prevX) * event.tickDelta;
        double y = entity.prevY + (entity.getY() - entity.prevY) * event.tickDelta;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * event.tickDelta;

        double height = entity.getBoundingBox().maxY - entity.getBoundingBox().minY;
        if (target == Target.Head) y += height;
        else if (target == Target.Body) y += height / 2;

        drawLine(getCameraVector(), x, y, z, color, event);
        if (stem) Renderer.LINES.line(x, entity.getY(), z, x, entity.getY() + height, z, color);
    }

    public static void drawTracerToPos(BlockPos pos, Color color, RenderEvent event) {
        drawLine(getCameraVector(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5f, color, event);
    }

    public static void drawLine(Vec3d vec, double x2, double y2, double z2, Color color, RenderEvent event) {
        Renderer.LINES.line(
                vec.x - (mc.gameRenderer.getCamera().getPos().x - event.offsetX),
                vec.y - (mc.gameRenderer.getCamera().getPos().y - event.offsetY),
                vec.z - (mc.gameRenderer.getCamera().getPos().z - event.offsetZ),
                x2, y2, z2, color);
    }

    public static void drawTracerToBlockEntity(BlockEntity blockEntity, Color color, RenderEvent event) {
        drawTracerToPos(blockEntity.getPos(), color, event);
    }
}

