package me.jellysquid.mods.lithium.common.world.chunk.heightmap;

import me.jellysquid.mods.lithium.mixin.world.combined_heightmap_update.HeightmapAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Objects;
import java.util.function.Predicate;

public class CombinedHeightmapUpdate {
    public static void updateHeightmaps(Heightmap heightmap0, Heightmap heightmap1, Heightmap heightmap2, Heightmap heightmap3, WorldChunk worldChunk, final int x, final int y, final int z, BlockState state) {
        final int height0 = heightmap0.get(x, z);
        final int height1 = heightmap1.get(x, z);
        final int height2 = heightmap2.get(x, z);
        final int height3 = heightmap3.get(x, z);
        int heightmapsToUpdate = 4;
        if (y + 2 <= height0) {
            heightmap0 = null;
            heightmapsToUpdate--;
        }
        if (y + 2 <= height1) {
            heightmap1 = null;
            heightmapsToUpdate--;
        }
        if (y + 2 <= height2) {
            heightmap2 = null;
            heightmapsToUpdate--;
        }
        if (y + 2 <= height3) {
            heightmap3 = null;
            heightmapsToUpdate--;
        }

        if (heightmapsToUpdate == 0) {
            return;
        }

        Predicate<BlockState> blockPredicate0 = heightmap0 == null ? null : Objects.requireNonNull(((HeightmapAccessor) heightmap0).getBlockPredicate());
        Predicate<BlockState> blockPredicate1 = heightmap1 == null ? null : Objects.requireNonNull(((HeightmapAccessor) heightmap1).getBlockPredicate());
        Predicate<BlockState> blockPredicate2 = heightmap2 == null ? null : Objects.requireNonNull(((HeightmapAccessor) heightmap2).getBlockPredicate());
        Predicate<BlockState> blockPredicate3 = heightmap3 == null ? null : Objects.requireNonNull(((HeightmapAccessor) heightmap3).getBlockPredicate());

        if (heightmap0 != null) {
            if (blockPredicate0.test(state)) {
                if (y >= height0) {
                    ((HeightmapAccessor) heightmap0).callSet(x, z, y + 1);
                }
                heightmap0 = null;
                heightmapsToUpdate--;
            } else if (height0 != y + 1) {
                heightmap0 = null;
                heightmapsToUpdate--;
            }
        }
        if (heightmap1 != null) {
            if (blockPredicate1.test(state)) {
                if (y >= height1) {
                    ((HeightmapAccessor) heightmap1).callSet(x, z, y + 1);
                }
                heightmap1 = null;
                heightmapsToUpdate--;
            } else if (height1 != y + 1) {
                heightmap1 = null;
                heightmapsToUpdate--;
            }
        }
        if (heightmap2 != null) {
            if (blockPredicate2.test(state)) {
                if (y >= height2) {
                    ((HeightmapAccessor) heightmap2).callSet(x, z, y + 1);
                }
                heightmap2 = null;
                heightmapsToUpdate--;
            } else if (height2 != y + 1) {
                heightmap2 = null;
                heightmapsToUpdate--;
            }
        }
        if (heightmap3 != null) {
            if (blockPredicate3.test(state)) {
                if (y >= height3) {
                    ((HeightmapAccessor) heightmap3).callSet(x, z, y + 1);
                }
                heightmap3 = null;
                heightmapsToUpdate--;
            } else if (height3 != y + 1) {
                heightmap3 = null;
                heightmapsToUpdate--;
            }
        }


        if (heightmapsToUpdate == 0) {
            return;
        }

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int bottomY = worldChunk.getBottomY();

        for (int searchY = y - 1; searchY >= bottomY && heightmapsToUpdate > 0; --searchY) {
            mutable.set(x, searchY, z);
            BlockState blockState = worldChunk.getBlockState(mutable);
            if (heightmap0 != null && blockPredicate0.test(blockState)) {
                ((HeightmapAccessor) heightmap0).callSet(x, z, searchY + 1);
                heightmap0 = null;
                heightmapsToUpdate--;
            }
            if (heightmap1 != null && blockPredicate1.test(blockState)) {
                ((HeightmapAccessor) heightmap1).callSet(x, z, searchY + 1);
                heightmap1 = null;
                heightmapsToUpdate--;
            }
            if (heightmap2 != null && blockPredicate2.test(blockState)) {
                ((HeightmapAccessor) heightmap2).callSet(x, z, searchY + 1);
                heightmap2 = null;
                heightmapsToUpdate--;
            }
            if (heightmap3 != null && blockPredicate3.test(blockState)) {
                ((HeightmapAccessor) heightmap3).callSet(x, z, searchY + 1);
                heightmap3 = null;
                heightmapsToUpdate--;
            }
        }
        if (heightmap0 != null) {
            ((HeightmapAccessor) heightmap0).callSet(x, z, bottomY);
        }
        if (heightmap1 != null) {
            ((HeightmapAccessor) heightmap1).callSet(x, z, bottomY);
        }
        if (heightmap2 != null) {
            ((HeightmapAccessor) heightmap2).callSet(x, z, bottomY);
        }
        if (heightmap3 != null) {
            ((HeightmapAccessor) heightmap3).callSet(x, z, bottomY);
        }
    }
}
