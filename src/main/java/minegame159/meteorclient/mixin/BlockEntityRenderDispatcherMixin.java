/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IBlockEntityType;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.XRay;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void onRenderEntity(E blockEntity, float tickDelta, MatrixStack matrix, VertexConsumerProvider vertexConsumerProvider, CallbackInfo info) {
        if (!Utils.blockRenderingBlockEntitiesInXray) return;

        XRay xray = ModuleManager.INSTANCE.get(XRay.class);

        for (Block block : ((IBlockEntityType) blockEntity.getType()).getBlocks()) {
            if (xray.isBlocked(block)) {
                info.cancel();
                break;
            }
        }
    }
}
