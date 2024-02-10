package me.jellysquid.mods.lithium.mixin.ai.poi.tasks;

import me.jellysquid.mods.lithium.common.util.POIRegistryEntries;
import me.jellysquid.mods.lithium.common.world.interests.iterator.SinglePointOfInterestTypeFilter;
import net.minecraft.entity.ai.brain.task.HideInHomeTask;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.function.Predicate;

@Mixin(HideInHomeTask.class)
public class HideInHomeTaskMixin {

    @Redirect(
            method = "method_46979",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/poi/PointOfInterestStorage;getPosition(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/world/poi/PointOfInterestStorage$OccupationStatus;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;)Ljava/util/Optional;"
            )
    )
    private static Optional<BlockPos> useFasterPOILookup(PointOfInterestStorage pointOfInterestStorage, Predicate<RegistryEntry<PointOfInterestType>> typePredicate, Predicate<BlockPos> positionPredicate, PointOfInterestStorage.OccupationStatus occupationStatus, BlockPos pos, int radius, Random random) {
        return pointOfInterestStorage.getPosition(new SinglePointOfInterestTypeFilter(POIRegistryEntries.HOME_ENTRY), positionPredicate, occupationStatus, pos, radius, random);
    }

    @Redirect(
            method = "method_46978",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/poi/PointOfInterestStorage;getPosition(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/world/poi/PointOfInterestStorage$OccupationStatus;)Ljava/util/Optional;"
            )
    )
    private static Optional<BlockPos> useFasterPOILookup(PointOfInterestStorage pointOfInterestStorage, Predicate<RegistryEntry<PointOfInterestType>> typePredicate, Predicate<BlockPos> posPredicate, BlockPos pos, int radius, PointOfInterestStorage.OccupationStatus occupationStatus) {
        return pointOfInterestStorage.getPosition(new SinglePointOfInterestTypeFilter(POIRegistryEntries.HOME_ENTRY), posPredicate, pos, radius, occupationStatus);
    }

}
