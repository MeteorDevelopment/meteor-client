package me.jellysquid.mods.lithium.mixin.util.block_tracking.block_listening;

import me.jellysquid.mods.lithium.common.entity.block_tracking.SectionedBlockChangeTracker;
import me.jellysquid.mods.lithium.common.util.deduplication.LithiumInterner;
import me.jellysquid.mods.lithium.common.util.deduplication.LithiumInternerWrapper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(World.class)
public class WorldMixin implements LithiumInternerWrapper<SectionedBlockChangeTracker> {
    private final LithiumInterner<SectionedBlockChangeTracker> blockChangeTrackers = new LithiumInterner<>();

    @Override
    public SectionedBlockChangeTracker getCanonical(SectionedBlockChangeTracker value) {
        return this.blockChangeTrackers.getCanonical(value);
    }

    @Override
    public void deleteCanonical(SectionedBlockChangeTracker value) {
        this.blockChangeTrackers.deleteCanonical(value);
    }
}
