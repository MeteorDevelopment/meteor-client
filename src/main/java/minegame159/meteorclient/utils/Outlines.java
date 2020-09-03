package minegame159.meteorclient.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Outlines {
    public static final List<Entity> ENTITIES = new ArrayList<>();

    public static boolean loadingOutlineShader;
    public static boolean renderingOutlines;

    private static Framebuffer outlinesFbo;
    private static ShaderEffect outlinesShader;

    public static void load() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (outlinesShader != null) {
                outlinesShader.close();
            }

            loadingOutlineShader = true;
            outlinesShader = new ShaderEffect(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), new Identifier("meteor-client", "shaders/post/my_entity_outline.json"));
            outlinesShader.setupDimensions(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
            outlinesFbo = outlinesShader.getSecondaryTarget("final");
            loadingOutlineShader = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void render(EntityRenderDispatcher entityRenderDispatcher, Frustum frustum, Camera camera, float tickDelta, MatrixStack matrices, BufferBuilderStorage bufferBuilders) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (ENTITIES.size() > 0) {
            outlinesFbo.clear(MinecraftClient.IS_SYSTEM_MAC);
            mc.getFramebuffer().beginWrite(false);

            OutlineVertexConsumerProvider immediate = bufferBuilders.getOutlineVertexConsumers();
            double camX = camera.getPos().getX();
            double camY = camera.getPos().getY();
            double camZ = camera.getPos().getZ();


            RenderSystem.depthFunc(519);
            RenderSystem.disableFog();
            DiffuseLighting.disable();

            renderingOutlines = true;
            for (Entity entity : ENTITIES) {
                int k = entity.getTeamColorValue();
                int t = k >> 16 & 255;
                int u = k >> 8 & 255;
                int w = k & 255;
                immediate.setColor(t, u, w, (k >> 24) & 0x000000FF);

                renderEntity(entityRenderDispatcher, entity, camX, camY, camZ, tickDelta, matrices, immediate);
            }
            renderingOutlines = false;

            DiffuseLighting.enable();
            RenderSystem.depthMask(false);

            outlinesShader.render(tickDelta);
            mc.getFramebuffer().beginWrite(false);

            RenderSystem.enableLighting();
            RenderSystem.depthMask(true);
            RenderSystem.enableFog();
            //RenderSystem.enableBlend();
            RenderSystem.enableColorMaterial();
            RenderSystem.depthFunc(515);
            RenderSystem.enableDepthTest();
            RenderSystem.enableAlphaTest();

            ENTITIES.clear();
        }
    }

    private static void renderEntity(EntityRenderDispatcher entityRenderDispatcher, Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrix, VertexConsumerProvider vertexConsumers) {
        double d = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
        double e = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY());
        double f = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());
        float g = MathHelper.lerp(tickDelta, entity.prevYaw, entity.yaw);
        entityRenderDispatcher.render(entity, d - cameraX, e - cameraY, f - cameraZ, g, tickDelta, matrix, vertexConsumers, entityRenderDispatcher.getLight(entity, tickDelta));
    }

    public static void renderFbo() {
        if (ENTITIES.size() > 0) {
            MinecraftClient mc = MinecraftClient.getInstance();

            outlinesFbo.draw(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false);
        }
    }

    public static void onResized(int width, int height) {
        if (outlinesShader != null) outlinesShader.setupDimensions(width, height);
    }

    public static Framebuffer getFramebuffer() {
        return outlinesFbo;
    }
}
