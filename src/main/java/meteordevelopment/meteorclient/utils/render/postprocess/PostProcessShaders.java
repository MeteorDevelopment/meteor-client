package meteordevelopment.meteorclient.utils.render.postprocess;

import meteordevelopment.meteorclient.utils.PreInit;
import net.minecraft.client.render.VertexConsumerProvider;

public class PostProcessShaders {
    public static final EntityShader CHAMS = new ChamsShader();
    public static final EntityShader ENTITY_OUTLINE = new EntityOutlineShader();
    public static final PostProcessShader STORAGE_OUTLINE = new StorageOutlineShader();

    public static boolean rendering;

    @PreInit
    public static void init() {
        ENTITY_OUTLINE.init("outline");
        STORAGE_OUTLINE.init("outline");
    }

    public static void beginRender() {
        CHAMS.beginRender();
        ENTITY_OUTLINE.beginRender();
        STORAGE_OUTLINE.beginRender();
    }

    public static void endRender() {
        CHAMS.endRender();
        ENTITY_OUTLINE.endRender();
    }

    public static void onResized(int width, int height) {
        CHAMS.onResized(width, height);
        ENTITY_OUTLINE.onResized(width, height);
        STORAGE_OUTLINE.onResized(width, height);
    }

    public static boolean isCustom(VertexConsumerProvider vcp) {
        return vcp == CHAMS.vertexConsumerProvider || vcp == ENTITY_OUTLINE.vertexConsumerProvider;
    }
}
