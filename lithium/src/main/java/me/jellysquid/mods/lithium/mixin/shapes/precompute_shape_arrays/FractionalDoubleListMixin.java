package me.jellysquid.mods.lithium.mixin.shapes.precompute_shape_arrays;

import net.minecraft.util.shape.FractionalDoubleList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FractionalDoubleList.class)
public class FractionalDoubleListMixin {
    @Shadow
    @Final
    private int sectionCount;

    private double scale;

    @Inject(method = "<init>(I)V", at = @At("RETURN"))
    public void initScale(int sectionCount, CallbackInfo ci) {
        this.scale = 1.0D / this.sectionCount;
    }

    /**
     * @author JellySquid
     * @reason Replace division with multiplication
     */
    @Overwrite
    public double getDouble(int position) {
        return position * this.scale;
    }
}
