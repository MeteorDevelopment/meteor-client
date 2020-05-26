package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.mixininterface.IGameRenderer;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.UnfocusedCPU;
import minegame159.meteorclient.modules.render.NoHurtCam;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements IGameRenderer {
    @Shadow @Final private MinecraftClient client;

    @Shadow @Final private Camera camera;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(float tickDelta, long startTime, boolean tick, CallbackInfo info) {
        if (ModuleManager.INSTANCE.isActive(UnfocusedCPU.class) && !client.isWindowFocused()) info.cancel();
    }

    @Inject(at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = { "ldc=hand" }), method = "renderCenter")
    public void onRenderCenter(float tickDelta, long endTime, CallbackInfo info) {
        if (!Utils.canUpdate()) return;

        client.getProfiler().swap("meteor-client_render");

        GlStateManager.pushMatrix();
        double px = camera.getPos().x;
        double py = camera.getPos().y;
        double pz = camera.getPos().z;
        GlStateManager.color4f(1, 1, 1, 1);
        RenderUtils.beginLines(-px, -py, -pz);
        RenderUtils.beginQuads(-px, -py, -pz);

        MeteorClient.EVENT_BUS.post(EventStore.renderEvent(tickDelta, px, py, pz));

        GlStateManager.disableLighting();
        GlStateManager.disableTexture();
        GlStateManager.disableDepthTest();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlphaTest();
        GlStateManager.lineWidth(1);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        RenderUtils.endQuads();
        RenderUtils.endLines();
        GlStateManager.popMatrix();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.lineWidth(1);
        GlStateManager.disableBlend();
        GlStateManager.enableDepthTest();
        GlStateManager.enableTexture();
        GlStateManager.enableLighting();
    }

    @Inject(method = "bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onBobViewWhenHurt(float tickDelta, CallbackInfo info) {
        if (ModuleManager.INSTANCE.isActive(NoHurtCam.class)) info.cancel();
    }

    @Override
    public Camera getCamera() {
        return camera;
    }
}
