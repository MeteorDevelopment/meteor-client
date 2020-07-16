package minegame159.meteorclient.mixin;

import minegame159.meteorclient.mixininterface.IBlockEntityType;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.XRay;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
    @Inject(method = "renderEntity(Lnet/minecraft/block/entity/BlockEntity;DDDFIZ)V", at = @At("HEAD"), cancellable = true)
    private void onRenderEntity(BlockEntity blockEntity, double xOffset, double yOffset, double zOffset, float tickDelta, int blockBreakStage, boolean bl, CallbackInfo info) {
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
