package minegame159.meteorclient.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class MyRenderLayer extends RenderPhase {
    private static final RenderPhase.Target MY_OUTLINE_TARGET = new RenderPhase.Target("meteor_outline_target", () -> {
        Outlines.getFramebuffer().beginWrite(false);
    }, () -> {
        MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
    });

    public MyRenderLayer(String name, Runnable startAction, Runnable endAction) {
        super(name, startAction, endAction);
    }

    public static RenderLayer getOutlineRenderLayer(Identifier texture) {
        return RenderLayer.of("outline", VertexFormats.POSITION_COLOR_TEXTURE, 7, 256, RenderLayer.MultiPhaseParameters.builder().texture(new RenderPhase.Texture(texture, false, false)).cull(DISABLE_CULLING).depthTest(ALWAYS_DEPTH_TEST).alpha(ONE_TENTH_ALPHA).texturing(OUTLINE_TEXTURING).fog(NO_FOG).target(MY_OUTLINE_TARGET).build(RenderLayer.OutlineMode.IS_OUTLINE));
    }
}
