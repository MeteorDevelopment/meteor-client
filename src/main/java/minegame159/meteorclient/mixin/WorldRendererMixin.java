package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.BlockSelection;
import minegame159.meteorclient.modules.render.NoRender;
import minegame159.meteorclient.utils.Outlines;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow public abstract void tick();

    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    @Inject(method = "loadEntityOutlineShader", at = @At("TAIL"))
    private void onLoadEntityOutlineShader(CallbackInfo info) {
        Outlines.load();
    }

    @Inject(method = "checkEmpty", at = @At("HEAD"), cancellable = true)
    private void onCheckEmpty(MatrixStack matrixStack, CallbackInfo info) {
        info.cancel();
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void onRenderWeather(LightmapTextureManager manager, float f, double d, double e, double g, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(NoRender.class).noWeather()) info.cancel();
    }

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void onRenderEntitiesHead(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrix, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        Utils.blockRenderingBlockEntitiesInXray = true;
    }

    @Inject(method = "renderEntity", at = @At("TAIL"))
    private void onRenderEntitiesTail(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrix, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        Utils.blockRenderingBlockEntitiesInXray = false;
    }
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo info) {
        Frustum frustum = new Frustum(matrices.peek().getModel(), matrix4f);
        frustum.setPosition(camera.getPos().x, camera.getPos().y, camera.getPos().z);

        Outlines.render(entityRenderDispatcher, frustum, camera, tickDelta, matrices, bufferBuilders);
    }

    @Inject(method = "drawEntityOutlinesFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(IIZ)V"))
    private void onDrawEntityOutlinesFramebuffer(CallbackInfo info) {
        Outlines.renderFbo();
    }

    @Inject(method = "onResized", at = @At("HEAD"))
    private void onResized(int i, int j, CallbackInfo info) {
        Outlines.onResized(i, j);
    }

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedBlockOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState, CallbackInfo info) {
        if (ModuleManager.INSTANCE.isActive(BlockSelection.class)) info.cancel();
    }
}
