package meteordevelopment.meteorclient.utils.render.postprocess;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.FullScreenRenderer;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.entity.Entity;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.glfwGetTime;

public abstract class PostProcessShader {
    public OutlineVertexConsumerProvider vertexConsumerProvider;
    public Framebuffer framebuffer;
    protected RenderPipeline pipeline;

    public void init(RenderPipeline pipeline) {
        vertexConsumerProvider = new OutlineVertexConsumerProvider(mc.getBufferBuilders().getEntityVertexConsumers());
        framebuffer = new SimpleFramebuffer(MeteorClient.NAME + " PostProcessShader", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), true);
        this.pipeline = pipeline;
    }

    protected abstract boolean shouldDraw();
    public abstract boolean shouldDraw(Entity entity);

    protected void preDraw() {}
    protected void postDraw() {}

    protected abstract void setupPass(RenderPass pass);

    public boolean beginRender() {
        return shouldDraw();
    }

    public void endRender(Runnable draw) {
        if (!shouldDraw()) return;

        preDraw();
        draw.run();
        postDraw();

        MeshRenderer.begin()
            .attachments(mc.getFramebuffer())
            .pipeline(pipeline)
            .mesh(FullScreenRenderer.mesh)
            .setupCallback(pass -> {
                pass.bindSampler("u_Texture", framebuffer.getColorAttachment());
                pass.setUniform("u_Size", (float) mc.getWindow().getFramebufferWidth(), (float) mc.getWindow().getFramebufferHeight());
                pass.setUniform("u_Time", (float) glfwGetTime());

                setupPass(pass);
            })
            .end();
    }

    public void onResized(int width, int height) {
        if (framebuffer == null) return;
        framebuffer.resize(width, height);
    }
}
