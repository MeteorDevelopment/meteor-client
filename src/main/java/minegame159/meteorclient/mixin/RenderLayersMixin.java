package minegame159.meteorclient.mixin;

import minegame159.meteorclient.utils.MyRenderLayer;
import minegame159.meteorclient.utils.Outlines;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderLayers.class)
public class RenderLayersMixin {
    @Inject(method = "getItemLayer", at = @At("INVOKE"), cancellable = true)
    private static void onGetItemLayer(ItemStack itemStack, boolean direct, CallbackInfoReturnable<RenderLayer> info) {
        if (Outlines.renderingOutlines) {
            info.setReturnValue(MyRenderLayer.getOutlineRenderLayer(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
        }
    }

    @Inject(method = "getEntityBlockLayer", at = @At("INVOKE"), cancellable = true)
    private static void onGetEntityBlockLayer(BlockState state, boolean direct, CallbackInfoReturnable<RenderLayer> info) {
        if (Outlines.renderingOutlines) {
            info.setReturnValue(MyRenderLayer.getOutlineRenderLayer(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
        }
    }
}
