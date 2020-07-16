package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.NoRender;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import minegame159.meteorclient.modules.render.BlockSelection;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void onRenderWeather(LightmapTextureManager manager, float f, double d, double e, double g, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noWeather()) info.cancel();
    }

    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void onRenderEntitiesHead(Camera camera, VisibleRegion visibleRegion, float tickDelta, CallbackInfo info) {
        Utils.blockRenderingBlockEntitiesInXray = true;
    }

    @Inject(method = "renderEntities", at = @At("TAIL"))
    private void onRenderEntitiesTail(Camera camera, VisibleRegion visibleRegion, float tickDelta, CallbackInfo info) {
        Utils.blockRenderingBlockEntitiesInXray = false;
    }

    @Inject(method = "drawHighlightedBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedBlockOutline(Camera camera, HitResult hit, int renderPass, CallbackInfo info) {
        if (ModuleManager.INSTANCE.isActive(BlockSelection.class)) info.cancel();
    }
}
