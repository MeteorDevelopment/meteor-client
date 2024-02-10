package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.furnace;

import me.jellysquid.mods.lithium.common.block.entity.SleepingBlockEntity;
import me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.WrappedBlockEntityTickInvokerAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin extends BlockEntity implements SleepingBlockEntity {

    @Shadow
    protected abstract boolean isBurning();

    @Shadow
    int cookTime;
    private WrappedBlockEntityTickInvokerAccessor tickWrapper = null;
    private BlockEntityTickInvoker sleepingTicker = null;

    public AbstractFurnaceBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public WrappedBlockEntityTickInvokerAccessor getTickWrapper() {
        return tickWrapper;
    }

    @Override
    public void setTickWrapper(WrappedBlockEntityTickInvokerAccessor tickWrapper) {
        this.tickWrapper = tickWrapper;
        this.setSleepingTicker(null);
    }

    @Override
    public BlockEntityTickInvoker getSleepingTicker() {
        return sleepingTicker;
    }

    @Override
    public void setSleepingTicker(BlockEntityTickInvoker sleepingTicker) {
        this.sleepingTicker = sleepingTicker;
    }

    @Inject(method = "tick", at = @At("RETURN" ))
    private static void checkSleep(World world, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        ((AbstractFurnaceBlockEntityMixin) (Object) blockEntity).checkSleep(state);
    }

    private void checkSleep(BlockState state) {
        if (!this.isBurning() && this.cookTime == 0 && (state.isOf(Blocks.FURNACE) || state.isOf(Blocks.BLAST_FURNACE) || state.isOf(Blocks.SMOKER)) && this.world != null) {
            this.startSleeping();
        }
    }

    @Inject(method = "readNbt", at = @At("RETURN" ))
    private void wakeUpAfterFromTag(CallbackInfo ci) {
        if (this.isSleeping() && this.world != null && !this.world.isClient) {
            this.wakeUpNow();
        }
    }

    @Override
    @Intrinsic
    public void markDirty() {
        super.markDirty();
    }

    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference"})
    @Inject(method = "markDirty()V", at = @At("RETURN"))
    private void wakeOnMarkDirty(CallbackInfo ci) {
        if (this.isSleeping() && this.world != null && !this.world.isClient) {
            this.wakeUpNow();
        }
    }

}
