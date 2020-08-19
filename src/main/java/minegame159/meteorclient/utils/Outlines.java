package minegame159.meteorclient.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlProgramManager;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

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

            if (GlProgramManager.getInstance() == null) {
                GlProgramManager.init();
            }

            if (outlinesShader != null) {
                outlinesShader.close();
            }

            loadingOutlineShader = true;
            outlinesShader = new ShaderEffect(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), new Identifier("meteor-client", "shaders/post/my_entity_outline.json"));
            outlinesShader.setupDimensions(mc.window.getFramebufferWidth(), mc.window.getFramebufferHeight());
            outlinesFbo = outlinesShader.getSecondaryTarget("final");
            loadingOutlineShader = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void render(EntityRenderDispatcher entityRenderDispatcher, VisibleRegion visibleRegion, Camera camera, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (ENTITIES.size() > 0) {
            outlinesFbo.clear(MinecraftClient.IS_SYSTEM_MAC);

            GlStateManager.depthFunc(519);
            GlStateManager.disableFog();
            outlinesFbo.beginWrite(false);
            DiffuseLighting.disable();

            renderingOutlines = true;
            entityRenderDispatcher.setRenderOutlines(true);
            for (Entity entity : ENTITIES) {
                if (entityRenderDispatcher.shouldRender(entity, visibleRegion, camera.getPos().x, camera.getPos().y, camera.getPos().z)) entityRenderDispatcher.render(entity, tickDelta, false);
            }
            entityRenderDispatcher.setRenderOutlines(false);
            renderingOutlines = false;

            DiffuseLighting.enable();
            GlStateManager.depthMask(false);
            outlinesShader.render(tickDelta);
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
            GlStateManager.enableFog();
            //GlStateManager.enableBlend();
            GlStateManager.enableColorMaterial();
            GlStateManager.depthFunc(515);
            GlStateManager.enableDepthTest();
            GlStateManager.enableAlphaTest();

            mc.getFramebuffer().beginWrite(false);
            ENTITIES.clear();
        }
    }

    public static void renderFbo() {
        if (ENTITIES.size() > 0) {
            MinecraftClient mc = MinecraftClient.getInstance();

            outlinesFbo.drawInternal(mc.window.getFramebufferWidth(), mc.window.getFramebufferHeight(), false);
        }
    }

    public static void onResized(int width, int height) {
        if (outlinesShader != null) outlinesShader.setupDimensions(width, height);
    }
}
