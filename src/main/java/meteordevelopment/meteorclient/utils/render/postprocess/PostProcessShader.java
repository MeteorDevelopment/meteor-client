package meteordevelopment.meteorclient.utils.render.postprocess;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.utils.render.CustomOutlineVertexConsumerProvider;
import net.minecraft.client.gl.DynamicUniformStorage;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.entity.Entity;

import java.nio.ByteBuffer;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.glfwGetTime;

public abstract class PostProcessShader {
    public CustomOutlineVertexConsumerProvider vertexConsumerProvider;
    public Framebuffer framebuffer;
    protected RenderPipeline pipeline;

    public void init(RenderPipeline pipeline) {
        if (vertexConsumerProvider == null) vertexConsumerProvider = new CustomOutlineVertexConsumerProvider();
        if (framebuffer == null) framebuffer = new SimpleFramebuffer(MeteorClient.NAME + " PostProcessShader", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), true);

        this.pipeline = pipeline;
    }

    protected abstract boolean shouldDraw();
    public abstract boolean shouldDraw(Entity entity);

    protected void preDraw() {}
    protected void postDraw() {}

    protected abstract void setupPass(MeshRenderer renderer);

    public boolean beginRender() {
        return shouldDraw();
    }

    public void endRender(Runnable draw) {
        if (!shouldDraw()) return;

        preDraw();
        draw.run();
        postDraw();

        var renderer = MeshRenderer.begin()
            .attachments(mc.getFramebuffer())
            .pipeline(pipeline)
            .fullscreen()
            .uniform("PostData", UNIFORM_STORAGE.write(new UniformData(
                (float) mc.getWindow().getFramebufferWidth(), (float) mc.getWindow().getFramebufferHeight(),
                (float) glfwGetTime()
            )))
            .sampler("u_Texture", framebuffer.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));

        setupPass(renderer);

        renderer.end();
    }

    public void onResized(int width, int height) {
        if (framebuffer == null) return;
        framebuffer.resize(width, height);
    }

    // Uniforms

    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
        .putVec2()
        .putFloat()
        .get();

    private static final DynamicUniformStorage<UniformData> UNIFORM_STORAGE = new DynamicUniformStorage<>("Meteor - Post UBO", UNIFORM_SIZE, 16);

    public static void flipFrame() {
        UNIFORM_STORAGE.clear();
    }

    private record UniformData(float sizeX, float sizeY, float time) implements DynamicUniformStorage.Uploadable {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(sizeX, sizeY)
                .putFloat(time);
        }
    }
}
