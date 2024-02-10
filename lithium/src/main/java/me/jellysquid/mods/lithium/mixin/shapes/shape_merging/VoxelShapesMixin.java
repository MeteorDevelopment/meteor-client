package me.jellysquid.mods.lithium.mixin.shapes.shape_merging;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import me.jellysquid.mods.lithium.common.shapes.pairs.LithiumDoublePairList;
import net.minecraft.util.shape.PairList;
import net.minecraft.util.shape.VoxelShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VoxelShapes.class)
public class VoxelShapesMixin {
    /**
     * Replaces the returned list pair with our own optimized type.
     */
    @Inject(
            method = "createListPair(ILit/unimi/dsi/fastutil/doubles/DoubleList;Lit/unimi/dsi/fastutil/doubles/DoubleList;ZZ)Lnet/minecraft/util/shape/PairList;",
            at = @At(
                    shift = At.Shift.BEFORE,
                    value = "NEW",
                    target = "net/minecraft/util/shape/SimplePairList"
            ),
            cancellable = true
    )
    private static void injectCustomListPair(int size, DoubleList a, DoubleList b, boolean flag1, boolean flag2, CallbackInfoReturnable<PairList> cir) {
        cir.setReturnValue(new LithiumDoublePairList(a, b, flag1, flag2));
    }
}
