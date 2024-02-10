package me.jellysquid.mods.lithium.mixin.entity.inactive_navigations;

import me.jellysquid.mods.lithium.common.entity.NavigatingEntity;
import net.minecraft.entity.mob.DrownedEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.entity.mob.DrownedEntity$LeaveWaterGoal")
public class DrownedEntityLeaveWaterGoalMixin {
    @Shadow
    @Final
    private DrownedEntity drowned;

    @Inject(method = "start()V", at = @At(value = "RETURN"))
    private void updateInactivityState(CallbackInfo ci) {
        ((NavigatingEntity) this.drowned).updateNavigationRegistration();
    }
}
