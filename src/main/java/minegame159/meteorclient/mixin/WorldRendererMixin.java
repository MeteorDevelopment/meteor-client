package minegame159.meteorclient.mixin;

import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void onRenderEntitiesHead(Camera camera, VisibleRegion visibleRegion, float tickDelta, CallbackInfo info) {
        Utils.blockRenderingBlockEntitiesInXray = true;
    }

    @Inject(method = "renderEntities", at = @At("TAIL"))
    private void onRenderEntitiesTail(Camera camera, VisibleRegion visibleRegion, float tickDelta, CallbackInfo info) {
        Utils.blockRenderingBlockEntitiesInXray = false;
    }
}
