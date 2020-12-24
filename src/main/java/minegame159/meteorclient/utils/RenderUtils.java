package minegame159.meteorclient.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.rendering.Renderer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class RenderUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    //Items
    public static void drawItem(ItemStack itemStack, int x, int y) {
        mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);
        RenderSystem.enableBlend();
    }

    public static void drawItemWithCount(ItemStack itemStack, int x, int y) {
        mc.getItemRenderer().renderGuiItemIcon(itemStack, x, y);
        mc.getItemRenderer().renderGuiItemOverlay(mc.textRenderer, itemStack, x, y, null);
        RenderSystem.enableBlend();
    }


    //Tracers
    public enum TracerTarget {
        Head,
        Body,
        Feet
    }

    public static void drawTracerToEntity(Entity entity, Color color, RenderEvent event, TracerTarget target, boolean stem) {
        double x = entity.prevX + (entity.getX() - entity.prevX) * event.tickDelta;
        double y = entity.prevY + (entity.getY() - entity.prevY) * event.tickDelta;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * event.tickDelta;
        Vec3d vec1 = new Vec3d(0, 0, 1)
                .rotateX(-(float) Math.toRadians(mc.gameRenderer.getCamera().getPitch()))
                .rotateY(-(float) Math.toRadians(mc.gameRenderer.getCamera().getYaw()))
                .add(mc.gameRenderer.getCamera().getPos());

        double height = entity.getBoundingBox().maxY - entity.getBoundingBox().minY;

        if (target == TracerTarget.Head) y += height;
        else if (target == TracerTarget.Body) y += height / 2;

        Renderer.LINES.line(vec1.x - (mc.gameRenderer.getCamera().getPos().x - event.offsetX), vec1.y - (mc.gameRenderer.getCamera().getPos().y - event.offsetY), vec1.z - (mc.gameRenderer.getCamera().getPos().z - event.offsetZ), x, y, z, color);
        if (stem) Renderer.LINES.line(x, entity.getY(), z, x, entity.getY() + height, z, color);
    }

    public static void drawTracerToBlockEntity(BlockEntity blockEntity, Color color, RenderEvent event) {
        BlockPos pos = blockEntity.getPos();
        Vec3d vec1 = new Vec3d(0, 0, 1)
                .rotateX(-(float) Math.toRadians(mc.gameRenderer.getCamera().getPitch()))
                .rotateY(-(float) Math.toRadians(mc.gameRenderer.getCamera().getYaw()))
                .add(mc.gameRenderer.getCamera().getPos());
        Renderer.LINES.line(vec1.x - (mc.gameRenderer.getCamera().getPos().x - event.offsetX), vec1.y - (mc.gameRenderer.getCamera().getPos().y - event.offsetY), vec1.z - (mc.gameRenderer.getCamera().getPos().z - event.offsetZ), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5f, color);
    }

    public static void drawTracerToBlockPos(BlockPos pos, Color color, RenderEvent event) {
        Vec3d vec1 = new Vec3d(0, 0, 1)
                .rotateX(-(float) Math.toRadians(mc.gameRenderer.getCamera().getPitch()))
                .rotateY(-(float) Math.toRadians(mc.gameRenderer.getCamera().getYaw()))
                .add(mc.gameRenderer.getCamera().getPos());
        Renderer.LINES.line(vec1.x - (mc.gameRenderer.getCamera().getPos().x - event.offsetX), vec1.y - (mc.gameRenderer.getCamera().getPos().y - event.offsetY), vec1.z - (mc.gameRenderer.getCamera().getPos().z - event.offsetZ), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5f, color);
    }
}
