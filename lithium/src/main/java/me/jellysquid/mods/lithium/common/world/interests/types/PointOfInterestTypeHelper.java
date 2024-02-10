package me.jellysquid.mods.lithium.common.world.interests.types;

import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ChunkSection;

import java.util.Set;
import java.util.function.Predicate;

public class PointOfInterestTypeHelper {
    private static Predicate<BlockState> POI_BLOCKSTATE_PREDICATE;


    public static void init(Set<BlockState> types) {
        if (POI_BLOCKSTATE_PREDICATE != null) {
            throw new IllegalStateException("Already initialized");
        }

        POI_BLOCKSTATE_PREDICATE = types::contains;
    }

    public static boolean shouldScan(ChunkSection section) {
        return section.hasAny(POI_BLOCKSTATE_PREDICATE);
    }
}
