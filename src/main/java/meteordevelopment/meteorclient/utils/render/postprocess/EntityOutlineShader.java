package meteordevelopment.meteorclient.utils.render.postprocess;

import com.mojang.blaze3d.systems.RenderPass;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import net.minecraft.entity.Entity;

public class EntityOutlineShader extends EntityShader {
    private static ESP esp;

    public EntityOutlineShader() {
        init(MeteorRenderPipelines.POST_OUTLINE);
    }

    @Override
    protected boolean shouldDraw() {
        if (esp == null) esp = Modules.get().get(ESP.class);
        return esp.isShader();
    }

    @Override
    public boolean shouldDraw(Entity entity) {
        if (!shouldDraw()) return false;
        return !esp.shouldSkip(entity);
    }

    @Override
    protected void setupPass(RenderPass pass) {
        pass.setUniform("u_Width", esp.outlineWidth.get());
        pass.setUniform("u_FillOpacity", esp.fillOpacity.get().floatValue());
        pass.setUniform("u_ShapeMode", esp.shapeMode.get().ordinal());
        pass.setUniform("u_GlowMultiplier", esp.glowMultiplier.get().floatValue());
    }
}
