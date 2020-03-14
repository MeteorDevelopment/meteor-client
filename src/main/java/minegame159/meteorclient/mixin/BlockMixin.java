package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.BlockShouldRenderSideEvent;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.XRay;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {
    private XRay xray;

    private XRay getXray() {
        if (xray == null) xray = ModuleManager.INSTANCE.get(XRay.class);
        return xray;
    }

    @Inject(method = "getAmbientOcclusionLightLevel", at = @At("HEAD"), cancellable = true)
    private void onGetAmbientOcclusionLightLevel(BlockState state, BlockView view, BlockPos pos, CallbackInfoReturnable<Float> info) {
        if (getXray().isActive()) {
            info.setReturnValue(1f);
        }
    }

    @Inject(at = @At("HEAD"), method = "shouldDrawSide", cancellable = true)
    private static void onShouldDrawSide(BlockState state, BlockView view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> info) {
        BlockShouldRenderSideEvent e = EventStore.blockShouldRenderSideEvent(state);
        MeteorClient.eventBus.post(e);

        if (e.isCancelled()) info.setReturnValue(e.shouldRenderSide);
    }
}
