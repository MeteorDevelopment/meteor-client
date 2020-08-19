package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.BlockSelection;
import minegame159.meteorclient.modules.render.ESP;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlProgramManager;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    @Inject(method = "loadEntityOutlineShader", at = @At("TAIL"))
    private void onLoadEntityOutlineShader(CallbackInfo info) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (GlProgramManager.getInstance() == null) {
                GlProgramManager.init();
            }

            if (MeteorClient.outlinesShader != null) {
                MeteorClient.outlinesShader.close();
            }

            MeteorClient.loadingOutlineShader = true;
            MeteorClient.outlinesShader = new ShaderEffect(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), new Identifier("meteor-client", "shaders/post/my_entity_outline.json"));
            MeteorClient.outlinesShader.setupDimensions(mc.window.getFramebufferWidth(), mc.window.getFramebufferHeight());
            MeteorClient.outlinesFbo = MeteorClient.outlinesShader.getSecondaryTarget("final");
            MeteorClient.loadingOutlineShader = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void onRenderEntitiesHead(Camera camera, VisibleRegion visibleRegion, float tickDelta, CallbackInfo info) {
        Utils.blockRenderingBlockEntitiesInXray = true;
    }

    @Inject(method = "renderEntities", at = @At("TAIL"))
    private void onRenderEntitiesTail(Camera camera, VisibleRegion visibleRegion, float tickDelta, CallbackInfo info) {
        Utils.blockRenderingBlockEntitiesInXray = false;
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;canDrawEntityOutlines()Z"))
    private void onRenderEntitiesOutlines(Camera camera, VisibleRegion visibleRegion, float tickDelta, CallbackInfo info) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (ESP.OUTLINE_ENTITIES.size() > 0) {
            MeteorClient.outlinesFbo.clear(MinecraftClient.IS_SYSTEM_MAC);

            GlStateManager.depthFunc(519);
            GlStateManager.disableFog();
            MeteorClient.outlinesFbo.beginWrite(false);
            DiffuseLighting.disable();

            MeteorClient.renderingOutlines = true;
            entityRenderDispatcher.setRenderOutlines(true);
            for (Entity entity : ESP.OUTLINE_ENTITIES) {
                if (entityRenderDispatcher.shouldRender(entity, visibleRegion, camera.getPos().x, camera.getPos().y, camera.getPos().z)) entityRenderDispatcher.render(entity, tickDelta, false);
            }
            entityRenderDispatcher.setRenderOutlines(false);
            MeteorClient.renderingOutlines = false;

            DiffuseLighting.enable();
            GlStateManager.depthMask(false);
            MeteorClient.outlinesShader.render(tickDelta);
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
            GlStateManager.enableFog();
            //GlStateManager.enableBlend();
            GlStateManager.enableColorMaterial();
            GlStateManager.depthFunc(515);
            GlStateManager.enableDepthTest();
            GlStateManager.enableAlphaTest();

            mc.getFramebuffer().beginWrite(false);
        }
    }

    @Inject(method = "drawEntityOutlinesFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;drawInternal(IIZ)V"))
    private void onDrawEntityOutlinesFramebuffer(CallbackInfo info) {
        if (ESP.OUTLINE_ENTITIES.size() > 0) {
            MinecraftClient mc = MinecraftClient.getInstance();

            MeteorClient.outlinesFbo.drawInternal(mc.window.getFramebufferWidth(), mc.window.getFramebufferHeight(), false);
        }
    }

    @Inject(method = "onResized", at = @At("HEAD"))
    private void onResized(int i, int j, CallbackInfo info) {
        if (MeteorClient.outlinesShader != null) MeteorClient.outlinesShader.setupDimensions(i, j);
    }

    @Inject(method = "drawHighlightedBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedBlockOutline(Camera camera, HitResult hit, int renderPass, CallbackInfo info) {
        if (ModuleManager.INSTANCE.isActive(BlockSelection.class)) info.cancel();
    }
}
