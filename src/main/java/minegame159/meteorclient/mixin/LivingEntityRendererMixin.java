package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.ChamsEvent;
import minegame159.meteorclient.events.EventStore;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    private ChamsEvent lastEvent;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
        lastEvent = EventStore.chamsEvent(livingEntity);
        MeteorClient.eventBus.post(lastEvent);

        if (lastEvent.enabled) {
            GL11.glEnable(32823);
            GL11.glPolygonOffset(1, -1000000);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info) {
        if (lastEvent.enabled) {
            GL11.glPolygonOffset(1, 1000000);
            GL11.glDisable(32823);
        }
    }
}
