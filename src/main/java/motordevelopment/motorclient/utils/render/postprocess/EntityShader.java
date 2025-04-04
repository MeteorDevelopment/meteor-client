package motordevelopment.motorclient.utils.render.postprocess;

import motordevelopment.motorclient.mixininterface.IWorldRenderer;

import static motordevelopment.motorclient.MotorClient.mc;

public abstract class EntityShader extends PostProcessShader {
    @Override
    protected void preDraw() {
        ((IWorldRenderer) mc.worldRenderer).motor$pushEntityOutlineFramebuffer(framebuffer);
    }

    @Override
    protected void postDraw() {
        ((IWorldRenderer) mc.worldRenderer).motor$popEntityOutlineFramebuffer();
    }

    public void endRender() {
        endRender(() -> vertexConsumerProvider.draw());
    }
}
