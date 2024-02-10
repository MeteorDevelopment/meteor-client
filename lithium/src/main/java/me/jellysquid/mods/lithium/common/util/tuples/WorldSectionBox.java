package me.jellysquid.mods.lithium.common.util.tuples;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

//Y values use coordinates, not indices (y=0 -> chunkY=0)
//upper bounds are EXCLUSIVE
public record WorldSectionBox(World world, int chunkX1, int chunkY1, int chunkZ1, int chunkX2, int chunkY2,
                              int chunkZ2) {
    public static WorldSectionBox entityAccessBox(World world, Box box) {
        int minX = ChunkSectionPos.getSectionCoord(box.minX - 2.0D);
        int minY = ChunkSectionPos.getSectionCoord(box.minY - 4.0D);
        int minZ = ChunkSectionPos.getSectionCoord(box.minZ - 2.0D);
        int maxX = ChunkSectionPos.getSectionCoord(box.maxX + 2.0D) + 1;
        int maxY = ChunkSectionPos.getSectionCoord(box.maxY) + 1;
        int maxZ = ChunkSectionPos.getSectionCoord(box.maxZ + 2.0D) + 1;
        return new WorldSectionBox(world, minX, minY, minZ, maxX, maxY, maxZ);
    }

    //Relevant block box: Entity hitbox expanded to all blocks it touches. Then expand the resulting box by 1 block in each direction.
    //Include all chunk sections that contain blocks inside the expanded box.
    public static WorldSectionBox relevantExpandedBlocksBox(World world, Box box) {
        int minX = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minX) - 1);
        int minY = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minY) - 1);
        int minZ = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minZ) - 1);
        int maxX = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.maxX) + 1) + 1;
        int maxY = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.maxY) + 1) + 1;
        int maxZ = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.maxZ) + 1) + 1;
        return new WorldSectionBox(world, minX, minY, minZ, maxX, maxY, maxZ);
    }
    //Like relevant blocks, but not expanded, because fluids never exceed the 1x1x1 volume of a block
    public static WorldSectionBox relevantFluidBox(World world, Box box) {
        int minX = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minX));
        int minY = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minY));
        int minZ = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minZ));
        int maxX = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.maxX)) + 1;
        int maxY = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.maxY)) + 1;
        int maxZ = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.maxZ)) + 1;
        return new WorldSectionBox(world, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public int numSections() {
        return (this.chunkX2 - this.chunkX1) * (this.chunkY2 - this.chunkY1) * (this.chunkZ2 - this.chunkZ1);
    }

    public boolean matchesRelevantBlocksBox(Box box) {
        return ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minX) - 1) == this.chunkX1 &&
                ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minY) - 1) == this.chunkY1 &&
                ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minZ) - 1) == this.chunkZ1 &&
                ChunkSectionPos.getSectionCoord(MathHelper.ceil(box.maxX) + 1) + 1 == this.chunkX2 &&
                ChunkSectionPos.getSectionCoord(MathHelper.ceil(box.maxY) + 1) + 1 == this.chunkY2 &&
                ChunkSectionPos.getSectionCoord(MathHelper.ceil(box.maxZ) + 1) + 1 == this.chunkZ2;
    }

}
