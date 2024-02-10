package me.jellysquid.mods.lithium.mixin.world.combined_heightmap_update;

import net.minecraft.block.BlockState;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Predicate;

@Mixin(Heightmap.class)
public interface HeightmapAccessor {
    @Invoker
    void callSet(int x, int z, int height);
    @Accessor("blockPredicate")
    Predicate<BlockState> getBlockPredicate();
}
