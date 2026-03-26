package meteordevelopment.meteorclient.utils.render.postprocess;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import net.minecraft.client.renderer.DynamicUniformStorage;
import java.nio.ByteBuffer;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.glfwGetTime;

public abstract class PostProcessShader {
    protected final RenderPipeline pipeline;
    public final RenderTarget framebuffer;

    protected PostProcessShader(RenderPipeline pipeline) {
        this.pipeline = pipeline;
        this.framebuffer = new TextureTarget(MeteorClient.NAME + " PostProcessShader " + this.getClass().getSimpleName(), mc.getWindow().getWidth(), mc.getWindow().getHeight(), true);
    }

    protected abstract boolean shouldDraw();

    protected void preDraw() {}
    protected void postDraw() {}

    protected abstract void setupPass(MeshRenderer renderer);

    public void clearTexture() {
        if (this.shouldDraw()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(framebuffer.getColorTexture(), 0);
        }
    }

    public void submitVertices(Runnable draw) {
        if (!shouldDraw()) return;

        preDraw();
        draw.run();
        postDraw();
    }

    public void render() {
        if (!shouldDraw()) return;

        var renderer = MeshRenderer.begin()
            .attachments(mc.getMainRenderTarget())
            .pipeline(pipeline)
            .fullscreen()
            .uniform("PostData", UNIFORM_STORAGE.writeUniform(new UniformData(
                (float) mc.getWindow().getWidth(), (float) mc.getWindow().getHeight(),
                (float) glfwGetTime()
            )))
            .sampler("u_Texture", framebuffer.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));

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
        UNIFORM_STORAGE.endFrame();
    }

    private record UniformData(float sizeX, float sizeY, float time) implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(sizeX, sizeY)
                .putFloat(time);
        }
    }
}
