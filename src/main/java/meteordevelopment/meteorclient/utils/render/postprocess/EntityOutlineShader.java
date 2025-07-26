package meteordevelopment.meteorclient.utils.render.postprocess;

import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import net.minecraft.entity.Entity;

public class EntityOutlineShader extends EntityShader {
    public EntityOutlineShader() {
        init(MeteorRenderPipelines.POST_OUTLINE);
    }

    @Override
    protected boolean shouldDraw() {
        return ESP.isShader();
    }

    @Override
    public boolean shouldDraw(Entity entity) {
        return !ESP.shouldSkip(entity);
    }

    @Override
    protected void setupPass(MeshRenderer renderer) {
        ESP esp = ESP.get();
        renderer.uniform("OutlineData", OutlineUniforms.write(
            esp.outlineWidth.get(),
            esp.fillOpacity.get().floatValue(),
            esp.shapeMode.get().ordinal(),
            esp.glowMultiplier.get().floatValue()
        ));
    }
}
