/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class RenderUtils {
    public static Vec3 center;
    public static final Matrix4f projection = new Matrix4f();

    private static final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private static final List<RenderBlock> renderBlocks = new ObjectArrayList<>();

    private RenderUtils() {
    }

    @PostInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(RenderUtils.class);
    }

    public static boolean isShaderPackInUse() {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    // Items
    public static void drawItem(GuiGraphics drawContext, ItemStack itemStack, int x, int y, float scale, boolean overlay, String countOverride, boolean disableGuiScale) {
        Matrix3x2fStack matrices = drawContext.pose();
        matrices.pushMatrix();

        if (disableGuiScale) {
            matrices.scale(1.0f / mc.getWindow().getGuiScale());
        }

        matrices.scale(scale, scale);

        int scaledX = (int) (x / scale);
        int scaledY = (int) (y / scale);

        drawContext.renderItem(itemStack, scaledX, scaledY);
        if (overlay) drawContext.renderItemDecorations(mc.font, itemStack, scaledX, scaledY, countOverride);

        matrices.popMatrix();
    }

    public static void drawItem(GuiGraphics drawContext, ItemStack itemStack, int x, int y, float scale, boolean overlay) {
        drawItem(drawContext, itemStack, x, y, scale, overlay, null, true);
    }

    public static void updateScreenCenter(Matrix4f projection, Matrix4f view) {
        RenderUtils.projection.set(projection);

        Matrix4f invProjection = new Matrix4f(projection).invert();
        Matrix4f invView = new Matrix4f(view).invert();

        Vector4f center4 = new Vector4f(0, 0, 0, 1).mul(invProjection).mul(invView);
        center4.div(center4.w);

        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        center = new Vec3(camera.x + center4.x, camera.y + center4.y, camera.z + center4.z);
    }

    public static void renderTickingBlock(BlockPos blockPos, Color sideColor, Color lineColor, ShapeMode shapeMode, int excludeDir, int duration, boolean fade, boolean shrink) {
        // Ensure there aren't multiple fading blocks in one pos
        renderBlocks.removeIf(next -> {
            if (next.pos.equals(blockPos)) {
                renderBlockPool.free(next);
                return true;
            } else {
                return false;
            }
        });

        renderBlocks.add(renderBlockPool.get().set(blockPos, sideColor, lineColor, shapeMode, excludeDir, duration, fade, shrink));
    }

    @EventHandler
    private static void onTick(TickEvent.Pre event) {
        if (renderBlocks.isEmpty()) return;

        renderBlocks.removeIf(next -> {
            next.tick();

            if (next.ticks <= 0) {
                renderBlockPool.free(next);
                return true;
            } else {
                return false;
            }
        });
    }

    @EventHandler
    private static void onRender(Render3DEvent event) {
        renderBlocks.forEach(block -> block.render(event));
    }

    public static class RenderBlock {
        public BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        public Color sideColor, lineColor;
        public ShapeMode shapeMode;
        public int excludeDir;

        public int ticks, duration;
        public boolean fade, shrink;

        public RenderBlock set(BlockPos blockPos, Color sideColor, Color lineColor, ShapeMode shapeMode, int excludeDir, int duration, boolean fade, boolean shrink) {
            pos.set(blockPos);
            this.sideColor = sideColor;
            this.lineColor = lineColor;
            this.shapeMode = shapeMode;
            this.excludeDir = excludeDir;
            this.fade = fade;
            this.shrink = shrink;
            this.ticks = duration;
            this.duration = duration;

            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event) {
            int preSideA = sideColor.a;
            int preLineA = lineColor.a;
            double x1 = pos.getX(), y1 = pos.getY(), z1 = pos.getZ(),
                x2 = pos.getX() + 1, y2 = pos.getY() + 1, z2 = pos.getZ() + 1;

            double d = (double) (ticks - event.tickDelta) / duration;

            if (fade) {
                sideColor.a = (int) (sideColor.a * d);
                lineColor.a = (int) (lineColor.a * d);
            }
            if (shrink) {
                x1 += d;
                y1 += d;
                z1 += d;
                x2 -= d;
                y2 -= d;
                z2 -= d;
            }

            event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, shapeMode, excludeDir);

            sideColor.a = preSideA;
            lineColor.a = preLineA;
        }
    }
}
