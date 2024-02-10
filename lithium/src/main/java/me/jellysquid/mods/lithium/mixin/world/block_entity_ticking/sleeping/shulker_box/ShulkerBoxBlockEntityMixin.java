package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.shulker_box;

import me.jellysquid.mods.lithium.common.block.entity.SleepingBlockEntity;
import me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.WrappedBlockEntityTickInvokerAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlockEntity.class)
public class ShulkerBoxBlockEntityMixin implements SleepingBlockEntity {
    @Shadow
    private ShulkerBoxBlockEntity.AnimationStage animationStage;
    @Shadow
    private float animationProgress;
    @Shadow
    private float prevAnimationProgress;
    private WrappedBlockEntityTickInvokerAccessor tickWrapper = null;
    private BlockEntityTickInvoker sleepingTicker = null;

    @Override
    public WrappedBlockEntityTickInvokerAccessor getTickWrapper() {
        return tickWrapper;
    }

    @Override
    public void setTickWrapper(WrappedBlockEntityTickInvokerAccessor tickWrapper) {
        this.tickWrapper = tickWrapper;
    }

    @Override
    public BlockEntityTickInvoker getSleepingTicker() {
        return sleepingTicker;
    }

    @Override
    public void setSleepingTicker(BlockEntityTickInvoker sleepingTicker) {
        this.sleepingTicker = sleepingTicker;
    }

    @Inject(
            method = "onSyncedBlockEvent",
            at = @At("HEAD")
    )
    private void wakeUpOnSyncedBlockEvent(int type, int data, CallbackInfoReturnable<Boolean> cir) {
        if (this.sleepingTicker != null) {
            this.wakeUpNow();
        }
    }

    @Inject(
            method = "updateAnimation",
            at = @At(value = "RETURN")
    )
    private void sleepOnAnimationEnd(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (this.animationStage == ShulkerBoxBlockEntity.AnimationStage.CLOSED && this.prevAnimationProgress == 0.0f && this.animationProgress == 0.0f) {
            this.startSleeping();
        }
    }
}
