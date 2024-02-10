package me.jellysquid.mods.lithium.mixin.ai.sensor.secondary_poi;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.SecondaryPointsOfInterestSensor;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SecondaryPointsOfInterestSensor.class)
public class SecondaryPointsOfInterestSensorMixin {

    @Inject(
            method = "sense(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/VillagerEntity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipUselessSense(ServerWorld serverWorld, VillagerEntity villagerEntity, CallbackInfo ci) {
        if (villagerEntity.getVillagerData().getProfession().secondaryJobSites().isEmpty()) {
            villagerEntity.getBrain().forget(MemoryModuleType.SECONDARY_JOB_SITE);
            ci.cancel();
        }
    }
}
