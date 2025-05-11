package meteordevelopment.meteorclient.utils.render.postprocess;

import com.mojang.blaze3d.systems.RenderPass;
import meteordevelopment.meteorclient.mixininterface.IMinecraftClient;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.StorageESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.entity.Entity;

public class StorageOutlineShader extends PostProcessShader {
    private static StorageESP storageESP;
    private Framebuffer mcFramebuffer;

    public StorageOutlineShader() {
        init(MeteorRenderPipelines.POST_OUTLINE);
    }

    @Override
    protected void preDraw() {
        mcFramebuffer = MinecraftClient.getInstance().getFramebuffer();
        ((IMinecraftClient) MinecraftClient.getInstance()).meteor$setFramebuffer(framebuffer);
    }

    @Override
    protected void postDraw() {
        ((IMinecraftClient) MinecraftClient.getInstance()).meteor$setFramebuffer(mcFramebuffer);
        mcFramebuffer = null;
    }

    @Override
    protected boolean shouldDraw() {
        if (storageESP == null) storageESP = Modules.get().get(StorageESP.class);
        return storageESP.isShader();
    }

    @Override
    public boolean shouldDraw(Entity entity) {
        return true;
    }

    @Override
    protected void setupPass(RenderPass pass) {
        pass.setUniform("u_Width", storageESP.outlineWidth.get());
        pass.setUniform("u_FillOpacity", storageESP.fillOpacity.get() / 255.0f);
        pass.setUniform("u_ShapeMode", storageESP.shapeMode.get().ordinal());
        pass.setUniform("u_GlowMultiplier", storageESP.glowMultiplier.get().floatValue());
    }
}
