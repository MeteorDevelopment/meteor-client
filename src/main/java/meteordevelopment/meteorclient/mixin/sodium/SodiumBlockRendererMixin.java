/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;

// TODO: Sodium
@Mixin(MinecraftClient.class)
//@Mixin(value = BlockRenderer.class, remap = false)
public class SodiumBlockRendererMixin {
    /*@Shadow
    @Final
    private ModelQuad cachedQuad;

    @Shadow
    @Final
    private BiomeColorBlender biomeColorBlender;


    @Inject(method = "renderQuad", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadViewMutable;setColor(II)V", shift = At.Shift.AFTER), cancellable = true)
    private void onRenderQuad(BlockRenderView world, BlockState state, BlockPos pos, ModelQuadSinkDelegate consumer, Vec3d offset, BlockColorProvider colorProvider, BakedQuad bakedQuad, QuadLightData light, ModelQuadFacing facing, CallbackInfo ci) {
        WallHack wallHack = Modules.get().get(WallHack.class);

        if(wallHack.isActive()) {
            if(wallHack.blocks.get().contains(state.getBlock())) {
                whRenderQuad(world, state, pos, consumer, offset, colorProvider, bakedQuad, light, facing, wallHack);
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void onRenderModel(BlockRenderView world, BlockState state, BlockPos pos, BakedModel model, ModelQuadSinkDelegate builder, boolean cull, long seed, CallbackInfoReturnable<Boolean> cir) {
        Xray xray = Modules.get().get(Xray.class);

        if (xray.isActive() && xray.isBlocked(state.getBlock())) {
            cir.setReturnValue(false);
        }
    }

    //https://github.com/CaffeineMC/sodium-fabric/blob/5af41c180e63590b7797b864393ef584a746eccd/src/main/java/me/jellysquid/mods/sodium/client/render/pipeline/BlockRenderer.java#L112
    //Copied from Sodium, for now, can't think of a better way, because of the nature of the locals, and for loop.
    //Mixin seems to freak out when I try to do this the "right" way - Wala (sobbing)
    private void whRenderQuad(BlockRenderView world, BlockState state, BlockPos pos, ModelQuadSinkDelegate consumer, Vec3d offset, BlockColorProvider colorProvider, BakedQuad bakedQuad, QuadLightData light, ModelQuadFacing facing, WallHack wallHack) {
        ModelQuadView src = (ModelQuadView)bakedQuad;
        ModelQuadOrientation order = ModelQuadOrientation.orient(light.br);
        ModelQuadViewMutable copy = cachedQuad;
        int norm = ModelQuadUtil.getFacingNormal(bakedQuad.getFace());
        int[] colors = null;
        if (bakedQuad.hasColor()) {
            colors = biomeColorBlender.getColors(colorProvider, world, state, pos, src);
        }

        for(int dstIndex = 0; dstIndex < 4; ++dstIndex) {
            int srcIndex = order.getVertexIndex(dstIndex);
            copy.setX(dstIndex, src.getX(srcIndex) + (float)offset.getX());
            copy.setY(dstIndex, src.getY(srcIndex) + (float)offset.getY());
            copy.setZ(dstIndex, src.getZ(srcIndex) + (float)offset.getZ());
            int newColor = ColorABGR.mul(colors != null ? colors[srcIndex] : -1, light.br[srcIndex]);
            int alpha = wallHack.opacity.get();
            int blue = ColorABGR.unpackBlue(newColor);
            int green = ColorABGR.unpackGreen(newColor);
            int red = ColorABGR.unpackRed(newColor);

            copy.setColor(dstIndex, ColorABGR.pack(red, green, blue, alpha));
            copy.setTexU(dstIndex, src.getTexU(srcIndex));
            copy.setTexV(dstIndex, src.getTexV(srcIndex));
            copy.setLight(dstIndex, light.lm[srcIndex]);
            copy.setNormal(dstIndex, norm);
            copy.setSprite(src.getSprite());
        }

        consumer.get(facing).write(copy);
    }*/
}
