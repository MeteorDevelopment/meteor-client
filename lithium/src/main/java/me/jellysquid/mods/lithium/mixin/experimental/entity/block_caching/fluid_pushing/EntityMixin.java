package me.jellysquid.mods.lithium.mixin.experimental.entity.block_caching.fluid_pushing;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import me.jellysquid.mods.lithium.common.entity.block_tracking.BlockCache;
import me.jellysquid.mods.lithium.common.entity.block_tracking.BlockCacheProvider;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Entity.class)
public abstract class EntityMixin implements BlockCacheProvider {
    @Shadow
    public abstract Box getBoundingBox();

    @Shadow
    protected Object2DoubleMap<TagKey<Fluid>> fluidHeight;

    @Shadow
    public abstract World getWorld();

    @Inject(
            method = "updateMovementInFluid",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipFluidSearchUsingCache(TagKey<Fluid> fluid, double speed, CallbackInfoReturnable<Boolean> cir) {
        BlockCache bc = this.getUpdatedBlockCache((Entity)(Object)this);
        double fluidHeight = bc.getStationaryFluidHeightOrDefault(fluid, -1d);
        if (fluidHeight != -1d) {
            this.fluidHeight.put(fluid, fluidHeight); //Note: If the region is unloaded in target method, this still puts 0. However, default return value is 0, and vanilla doesn't use any method that reveals this difference.
            boolean touchingFluid = fluidHeight != 0d;
            cir.setReturnValue(touchingFluid);
        }
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(
            method = "updateMovementInFluid", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;length()D", ordinal = 0, shift = At.Shift.BEFORE)
    )
    private void cacheFluidSearchResult(TagKey<Fluid> fluid, double speed, CallbackInfoReturnable<Boolean> cir, Box box, int i1, int i2, int i3, int i4, int i5, int i6, double fluidHeight, boolean isPushedbyFluids, boolean touchingFluid, Vec3d fluidPush, int i7) {
        BlockCache bc = this.getBlockCache();
        if (bc.isTracking() && fluidPush.lengthSquared() == 0d) {
            if (touchingFluid == (fluidHeight == 0d)) {
                throw new IllegalArgumentException("Expected fluid touching IFF fluid height is not 0! Fluid height: " + fluidHeight + " Touching fluid: " + touchingFluid + " Fluid Tag: " + fluid);
            }
            bc.setCachedFluidHeight(fluid, fluidHeight);
        }
    }
}
