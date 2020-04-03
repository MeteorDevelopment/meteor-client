package minegame159.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.NoHurtCam;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow @Final private Camera camera;

    @Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = { "ldc=hand" }))
    public void onRenderWorld(float tickDelta, long limitTime, MatrixStack matrix, CallbackInfo info) {
        if (!Utils.canUpdate()) return;

        client.getProfiler().swap("meteor-client_render");
        RenderSystem.disableLighting();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.lineWidth(1);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        RenderSystem.pushMatrix();
        double px = camera.getPos().x;
        double py = camera.getPos().y;
        double pz = camera.getPos().z;
        RenderSystem.multMatrix(matrix.peek().getModel());
        RenderSystem.color4f(1, 1, 1, 1);
        RenderUtils.beginLines(-px, -py, -pz);
        RenderUtils.beginQuads(-px, -py, -pz);

        MeteorClient.EVENT_BUS.post(EventStore.renderEvent(matrix, tickDelta, px, py, pz));

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

    @Inject(method = "bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void onBobViewWhenHurt(MatrixStack matrixStack, float f, CallbackInfo info) {
        if (ModuleManager.INSTANCE.isActive(NoHurtCam.class)) info.cancel();
    }
}
