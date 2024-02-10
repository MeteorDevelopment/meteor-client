package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping.campfire.lit;

import me.jellysquid.mods.lithium.common.block.entity.SleepingBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin extends BlockEntity implements SleepingBlockEntity {

    public CampfireBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(
            method = "litServerTick",
            at = @At("RETURN" ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void trySleepLit(World world, BlockPos pos, BlockState state, CampfireBlockEntity campfire, CallbackInfo ci, boolean hadProgress) {
        if (!hadProgress) {
            CampfireBlockEntityMixin self = (CampfireBlockEntityMixin) (Object) campfire;
            self.startSleeping();
        }
    }
}
