package minegame159.meteorclient.mixin;

import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.XRay;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockOcclusionCache.class, remap = false)
public class BlockOcculsionCacheMixin {
    @Inject(at = @At("HEAD"), method = "shouldDrawSide", cancellable = true)
    private void shouldDrawSide(BlockState selfState, BlockView view, BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> info) {
        XRay xray = ModuleManager.INSTANCE.get(XRay.class);

        if (xray.isActive()) {
            info.setReturnValue(!xray.isBlocked(selfState.getBlock()));
        }
    }
}
