package minegame159.meteorclient.mixin;

import minegame159.meteorclient.utils.MyRenderLayer;
import minegame159.meteorclient.utils.Outlines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemFrameEntityRenderer.class)
public class ItemFrameEntityRendererMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/TexturedRenderLayers;getEntitySolid()Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer onRenderGetEntitySolidProxy() {
        if (Outlines.renderingOutlines) return MyRenderLayer.getOutlineRenderLayer(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        return TexturedRenderLayers.getEntitySolid();
    }
}
