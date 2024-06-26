package meteordevelopment.meteorclient.utils.render.postprocess;

import meteordevelopment.meteorclient.mixin.WorldRendererAccessor;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public abstract class EntityShader extends PostProcessShader {
    private Framebuffer prevBuffer;

    @Override
    protected void preDraw() {
        WorldRenderer worldRenderer = mc.worldRenderer;
        WorldRendererAccessor wra = (WorldRendererAccessor) worldRenderer;
        prevBuffer = worldRenderer.getEntityOutlinesFramebuffer();
        wra.setEntityOutlinesFramebuffer(framebuffer);
    }

    @Override
    protected void postDraw() {
        if (prevBuffer == null) return;

        WorldRenderer worldRenderer = mc.worldRenderer;
        WorldRendererAccessor wra = (WorldRendererAccessor) worldRenderer;
        wra.setEntityOutlinesFramebuffer(prevBuffer);
        prevBuffer = null;
    }

    public void endRender() {
        endRender(() -> ((OutlineVertexConsumerProvider) vertexConsumerProvider).draw());
    }
}
