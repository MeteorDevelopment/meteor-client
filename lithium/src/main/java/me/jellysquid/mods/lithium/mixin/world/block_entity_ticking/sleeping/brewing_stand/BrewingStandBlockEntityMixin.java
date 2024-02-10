package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.brewing_stand;

import me.jellysquid.mods.lithium.common.block.entity.SleepingBlockEntity;
import me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.WrappedBlockEntityTickInvokerAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandBlockEntityMixin extends BlockEntity implements SleepingBlockEntity {

    @Shadow
    int brewTime;

    private WrappedBlockEntityTickInvokerAccessor tickWrapper = null;
    private BlockEntityTickInvoker sleepingTicker = null;

    public BrewingStandBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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

    @Inject(method = "tick", at = @At("HEAD"))
    private static void checkSleep(World world, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity, CallbackInfo ci) {
        ((BrewingStandBlockEntityMixin) (Object) blockEntity).checkSleep(state);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BrewingStandBlockEntity;markDirty(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"))
    private static void wakeUpOnMarkDirty(World world, BlockPos pos, BlockState state, BrewingStandBlockEntity blockEntity, CallbackInfo ci) {
        ((BrewingStandBlockEntityMixin) (Object) blockEntity).wakeUpNow();
    }

    private void checkSleep(BlockState state) {
        if (this.brewTime == 0 && state.isOf(Blocks.BREWING_STAND) && this.world != null) {
            this.startSleeping();
        }
    }

    @Inject(method = "readNbt", at = @At("RETURN"))
    private void wakeUpAfterFromTag(CallbackInfo ci) {
        if (this.isSleeping() && this.world != null && !this.world.isClient()) {
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
