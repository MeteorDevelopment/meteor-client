package me.jellysquid.mods.lithium.mixin.util.entity_movement_tracking;

import me.jellysquid.mods.lithium.common.entity.movement_tracker.EntityMovementTrackerSection;
import me.jellysquid.mods.lithium.common.entity.movement_tracker.MovementTrackerHelper;
import me.jellysquid.mods.lithium.common.entity.movement_tracker.ToggleableMovementTracker;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.EntityTrackingStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "net/minecraft/server/world/ServerEntityManager$Listener")
public class ServerEntityManagerListenerMixin<T extends EntityLike> implements ToggleableMovementTracker {
    @Shadow
    private EntityTrackingSection<T> section;
    @Shadow
    @Final
    private T entity;

    private int notificationMask;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(ServerEntityManager<?> outer, T entityLike, long l, EntityTrackingSection<T> entityTrackingSection, CallbackInfo ci) {
        this.notificationMask = MovementTrackerHelper.getNotificationMask(this.entity.getClass());

        //Fix #284 Summoned inventory minecarts do not immediately notify hoppers of their presence when created using summon command
        this.notifyMovementListeners();
    }

    @Inject(method = "updateEntityPosition()V", at = @At("RETURN"))
    private void updateEntityTrackerEngine(CallbackInfo ci) {
        this.notifyMovementListeners();
    }

    @Inject(
            method = "updateEntityPosition()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityTrackingSection;add(Lnet/minecraft/world/entity/EntityLike;)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onAddEntity(CallbackInfo ci, BlockPos blockPos, long newPos, EntityTrackingStatus entityTrackingStatus, EntityTrackingSection<T> entityTrackingSection) {
        this.notifyMovementListeners();
    }

    @Inject(
            method = "remove(Lnet/minecraft/entity/Entity$RemovalReason;)V",
            at = @At(
                    value = "HEAD"
            )
    )
    private void onRemoveEntity(Entity.RemovalReason reason, CallbackInfo ci) {
        this.notifyMovementListeners();
    }

    private void notifyMovementListeners() {
        if (this.notificationMask != 0) {
            ((EntityMovementTrackerSection) this.section).trackEntityMovement(this.notificationMask, ((Entity) this.entity).getEntityWorld().getTime());
        }
    }

    @Override
    public int setNotificationMask(int notificationMask) {
        int oldNotificationMask = this.notificationMask;
        this.notificationMask = notificationMask;
        return oldNotificationMask;
    }
}
