package motordevelopment.motorclient.utils.render.postprocess;

import motordevelopment.motorclient.renderer.GL;
import motordevelopment.motorclient.renderer.PostProcessRenderer;
import motordevelopment.motorclient.renderer.Shader;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.entity.Entity;

import static motordevelopment.motorclient.MotorClient.mc;
import static org.lwjgl.glfw.GLFW.glfwGetTime;

public abstract class PostProcessShader {
    public OutlineVertexConsumerProvider vertexConsumerProvider;
    public Framebuffer framebuffer;
    protected Shader shader;

    public void init(String frag) {
        vertexConsumerProvider = new OutlineVertexConsumerProvider(mc.getBufferBuilders().getEntityVertexConsumers());
        framebuffer = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), true);
        shader = new Shader("post-process/base.vert", "post-process/" + frag + ".frag");
    }

    protected abstract boolean shouldDraw();
    public abstract boolean shouldDraw(Entity entity);

    protected void preDraw() {}
    protected void postDraw() {}

    protected abstract void setUniforms();

    public void beginRender() {
        if (!shouldDraw()) return;

        framebuffer.clear();
        mc.getFramebuffer().beginWrite(false);
    }

    public void endRender(Runnable draw) {
        if (!shouldDraw()) return;

        preDraw();
        draw.run();
        postDraw();

        mc.getFramebuffer().beginWrite(false);

        GL.bindTexture(framebuffer.getColorAttachment(), 0);

        shader.bind();

        shader.set("u_Size", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        shader.set("u_Texture", 0);
        shader.set("u_Time", glfwGetTime());
        setUniforms();

        PostProcessRenderer.render();
    }

    public void onResized(int width, int height) {
        if (framebuffer == null) return;
        framebuffer.resize(width, height);
    }
}
