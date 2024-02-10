package me.jellysquid.mods.lithium.mixin.ai.poi;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.lithium.common.world.interests.types.PointOfInterestTypeHelper;
import net.minecraft.block.BlockState;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

/**
 * Replaces the backing map type with a faster collection type which uses reference equality.
 */
@Mixin(PointOfInterestTypes.class)
public class PointOfInterestTypesMixin {
    @Mutable
    @Shadow
    @Final
    private static Map<BlockState, PointOfInterestType> POI_STATES_TO_TYPE;

    static {
        POI_STATES_TO_TYPE = new Reference2ReferenceOpenHashMap<>(POI_STATES_TO_TYPE);

        PointOfInterestTypeHelper.init(POI_STATES_TO_TYPE.keySet());
    }
}
