package meteordevelopment.meteorclient.utils.render.postprocess;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.mixininterface.IWorldRenderer;

import java.util.OptionalInt;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public abstract class EntityShader extends PostProcessShader {
    @Override
    public boolean beginRender() {
        if (super.beginRender()) {
            RenderSystem.getDevice().createCommandEncoder().createRenderPass(framebuffer.getColorAttachment(), OptionalInt.of(0)).close();
            return true;
        }

        return false;
    }

    @Override
    protected void preDraw() {
        ((IWorldRenderer) mc.worldRenderer).meteor$pushEntityOutlineFramebuffer(framebuffer);
    }

    @Override
    protected void postDraw() {
        ((IWorldRenderer) mc.worldRenderer).meteor$popEntityOutlineFramebuffer();
    }

    public void endRender() {
        endRender(() -> vertexConsumerProvider.draw());
    }
}
