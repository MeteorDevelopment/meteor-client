package meteordevelopment.meteorclient.utils.render.postprocess;

import meteordevelopment.meteorclient.utils.PreInit;
import net.minecraft.client.render.VertexConsumerProvider;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PostProcessShaders {
    public static EntityShader CHAMS;
    public static EntityShader ENTITY_OUTLINE;
    public static PostProcessShader STORAGE_OUTLINE;

    public static boolean rendering;

    private PostProcessShaders() {
    }

    @PreInit
    public static void init() {
        CHAMS = new ChamsShader();
        ENTITY_OUTLINE = new EntityOutlineShader();
        STORAGE_OUTLINE = new StorageOutlineShader();
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
        if (mc == null) return;

        CHAMS.onResized(width, height);
        ENTITY_OUTLINE.onResized(width, height);
        STORAGE_OUTLINE.onResized(width, height);
    }

    public static boolean isCustom(VertexConsumerProvider vcp) {
        return vcp == CHAMS.vertexConsumerProvider || vcp == ENTITY_OUTLINE.vertexConsumerProvider;
    }
}
