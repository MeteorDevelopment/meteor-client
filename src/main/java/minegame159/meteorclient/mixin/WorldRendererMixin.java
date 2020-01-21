package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow private ClientWorld world;

    @Inject(at = @At("TAIL"), method = "render")
    public void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo info) {
        if (!Utils.canUpdate()) return;

        world.getProfiler().swap("meteor-client_render");
        RenderSystem.disableLighting();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(1);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(matrices.peek().getModel());
        RenderSystem.translated(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);
        RenderSystem.color4f(1, 1, 1, 1);
        RenderUtils.beginLines();
        RenderUtils.beginQuads();

        MeteorClient.eventBus.post(EventStore.renderEvent(tickDelta));

        RenderUtils.endQuads();
        RenderUtils.endLines();
        RenderSystem.popMatrix();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(1);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.enableLighting();
    }
}
