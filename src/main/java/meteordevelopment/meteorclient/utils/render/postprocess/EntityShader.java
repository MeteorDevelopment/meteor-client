package meteordevelopment.meteorclient.utils.render.postprocess;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import meteordevelopment.meteorclient.mixininterface.IWorldRenderer;
import meteordevelopment.meteorclient.utils.render.CustomOutlineVertexConsumerProvider;
import net.minecraft.world.entity.Entity;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public abstract class EntityShader extends PostProcessShader {
    public final CustomOutlineVertexConsumerProvider vertexConsumerProvider;

    protected EntityShader(RenderPipeline pipeline) {
        super(pipeline);
        this.vertexConsumerProvider = new CustomOutlineVertexConsumerProvider();
    }

    public abstract boolean shouldDraw(Entity entity);

    @Override
    protected void preDraw() {
        ((IWorldRenderer) mc.levelRenderer).meteor$pushEntityOutlineFramebuffer(framebuffer);
    }

    @Override
    protected void postDraw() {
        ((IWorldRenderer) mc.levelRenderer).meteor$popEntityOutlineFramebuffer();
    }

    public void submitVertices() {
        submitVertices(vertexConsumerProvider::draw);
    }
}
