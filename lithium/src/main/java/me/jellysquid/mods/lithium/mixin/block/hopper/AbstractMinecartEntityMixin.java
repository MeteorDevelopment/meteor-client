package me.jellysquid.mods.lithium.mixin.block.hopper;

import me.jellysquid.mods.lithium.common.entity.movement_tracker.ToggleableMovementTracker;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityChangeListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends Entity {

    public AbstractMinecartEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    private Vec3d beforeMoveOnRailPos;
    private int beforeMoveOnRailNotificationMask;

    @Inject(
            method = "moveOnRail",
            at = @At("HEAD")
    )
    private void avoidNotifyingMovementListeners(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (this instanceof Inventory) {
            this.beforeMoveOnRailPos = this.getPos();
            EntityChangeListener changeListener = ((EntityAccessor) this).getChangeListener();
            if (changeListener instanceof ToggleableMovementTracker toggleableMovementTracker) {
                this.beforeMoveOnRailNotificationMask = toggleableMovementTracker.setNotificationMask(0);
            }
        }
    }

    @Inject(
            method = "moveOnRail",
            at = @At("RETURN")
    )
    private void notifyMovementListeners(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (this instanceof Inventory) {
            EntityChangeListener changeListener = ((EntityAccessor) this).getChangeListener();
            if (changeListener instanceof ToggleableMovementTracker toggleableMovementTracker) {
                this.beforeMoveOnRailNotificationMask = toggleableMovementTracker.setNotificationMask(this.beforeMoveOnRailNotificationMask);

                if (!this.beforeMoveOnRailPos.equals(this.getPos())) {
                    changeListener.updateEntityPosition();
                }
            }
            this.beforeMoveOnRailPos = null;
        }
    }
}
