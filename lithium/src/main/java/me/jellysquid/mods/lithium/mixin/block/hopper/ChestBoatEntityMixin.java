package me.jellysquid.mods.lithium.mixin.block.hopper;

import me.jellysquid.mods.lithium.common.entity.movement_tracker.ToggleableMovementTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityChangeListener;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChestBoatEntity.class)
public abstract class ChestBoatEntityMixin extends Entity {
    public ChestBoatEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Intrinsic
    @Override
    public void tickRiding() {
        super.tickRiding();
    }

    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference"})
    @Redirect(
            method = "tickRiding()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;tickRiding()V")
    )
    private void tickRidingSummarizeMovementNotifications(Entity entity) {
        EntityChangeListener changeListener = ((EntityAccessor) this).getChangeListener();
        if (changeListener instanceof ToggleableMovementTracker toggleableMovementTracker) {
            Vec3d beforeTickPos = this.getPos();
            int beforeMovementNotificationMask = toggleableMovementTracker.setNotificationMask(0);

            super.tickRiding();

            toggleableMovementTracker.setNotificationMask(beforeMovementNotificationMask);

            if (!beforeTickPos.equals(this.getPos())) {
                changeListener.updateEntityPosition();
            }
        } else {
            super.tickRiding();
        }
    }
}
