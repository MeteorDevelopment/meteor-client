package meteordevelopment.meteorclient.utils.render.postprocess;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public abstract class PostProcessShader {
    protected final RenderPipeline pipeline;
    public final RenderTarget framebuffer;

    protected PostProcessShader(RenderPipeline pipeline) {
        this.pipeline = pipeline;
        this.framebuffer = new TextureTarget(MeteorClient.NAME + " PostProcessShader " + this.getClass().getSimpleName(), mc.getWindow().getWidth(), mc.getWindow().getHeight(),
            GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
    }

    protected abstract boolean shouldDraw();

    protected void preDraw() {
    }

    protected void postDraw() {
    }

    protected abstract void setupPass(MeshRenderer renderer);

    public void clearTexture() {
        if (this.shouldDraw()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(framebuffer.getColorTexture(), new Vector4f(0));
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
            .attachments(mc.gameRenderer.mainRenderTarget())
            .pipeline(pipeline)
            .fullscreen()
            .uniform("PostData", UNIFORM_STORAGE.writeData(new UniformData(
                (float) mc.getWindow().getWidth(), (float) mc.getWindow().getHeight(),
                (float) Blaze3D.getTime()
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

    // todo what should usage be? not just here but everywhere else also
    private static final DynamicGpuDataStorage<UniformData> UNIFORM_STORAGE = new DynamicGpuDataStorage<>("Meteor - Post UBO", UNIFORM_SIZE, GpuBuffer.USAGE_UNIFORM, 16);

    public static void flipFrame() {
        UNIFORM_STORAGE.endFrame();
    }

    private record UniformData(float sizeX, float sizeY, float time) implements DynamicGpuDataStorage.DynamicGpuData {
        @Override
        public void write(@NonNull ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(sizeX, sizeY)
                .putFloat(time);
        }
    }
}
