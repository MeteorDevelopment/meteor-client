/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.canvas;

/*import grondag.canvas.render.world.CanvasWorldRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BlockSelection;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;*/

// TODO: 1.19.3
//@Mixin(value = CanvasWorldRenderer.class, remap = false)
public class CanvasWorldRendererMixin {
    /*@ModifyVariable(method = "renderWorld", at = @At("LOAD"), name = "blockOutlines")
    private boolean renderWorld_blockOutlines(boolean blockOutlines) {
        if (Modules.get().isActive(BlockSelection.class)) return false;
        return blockOutlines;
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void onRenderHead(MatrixStack viewMatrixStack, float tickDelta, long frameStartNanos, boolean blockOutlines, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, net.minecraft.class_1159 projectionMatrix, CallbackInfo info) {
        PostProcessShaders.beginRender();
    }*/

    // Injected through ASM because mixins are fucking retarded and don't work outside of development environment for this one injection
    /*@Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V", shift = At.Shift.AFTER))
    private void onRenderOutlines(CallbackInfo info) {
        PostProcessShaders.endRender();
    }*/
}
