package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.BlockSelection;
import minegame159.meteorclient.utils.Outlines;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VisibleRegion;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    @Inject(method = "loadEntityOutlineShader", at = @At("TAIL"))
    private void onLoadEntityOutlineShader(CallbackInfo info) {
        Outlines.load();
    }

    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void onRenderEntitiesHead(Camera camera, VisibleRegion visibleRegion, float tickDelta, CallbackInfo info) {
        Utils.blockRenderingBlockEntitiesInXray = true;
    }

    @Inject(method = "renderEntities", at = @At("TAIL"))
    private void onRenderEntitiesTail(Camera camera, VisibleRegion visibleRegion, float tickDelta, CallbackInfo info) {
        Utils.blockRenderingBlockEntitiesInXray = false;
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;canDrawEntityOutlines()Z"))
    private void onRenderEntitiesOutlines(Camera camera, VisibleRegion visibleRegion, float tickDelta, CallbackInfo info) {
        Outlines.render(entityRenderDispatcher, visibleRegion, camera, tickDelta);
    }

    @Inject(method = "drawEntityOutlinesFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;drawInternal(IIZ)V"))
    private void onDrawEntityOutlinesFramebuffer(CallbackInfo info) {
        Outlines.renderFbo();
    }

    @Inject(method = "onResized", at = @At("HEAD"))
    private void onResized(int i, int j, CallbackInfo info) {
        Outlines.onResized(i, j);
    }

    @Inject(method = "drawHighlightedBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedBlockOutline(Camera camera, HitResult hit, int renderPass, CallbackInfo info) {
        if (ModuleManager.INSTANCE.isActive(BlockSelection.class)) info.cancel();
    }
}
