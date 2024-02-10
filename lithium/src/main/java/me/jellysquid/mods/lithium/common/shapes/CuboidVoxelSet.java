package me.jellysquid.mods.lithium.common.shapes;

import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelSet;

public class CuboidVoxelSet extends VoxelSet {
    private final int minX, minY, minZ, maxX, maxY, maxZ;

    protected CuboidVoxelSet(int xSize, int ySize, int zSize, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        super(xSize, ySize, zSize);

        this.minX = (int) Math.round(minX * xSize);
        this.maxX = (int) Math.round(maxX * xSize);
        this.minY = (int) Math.round(minY * ySize);
        this.maxY = (int) Math.round(maxY * ySize);
        this.minZ = (int) Math.round(minZ * zSize);
        this.maxZ = (int) Math.round(maxZ * zSize);
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x < this.maxX &&
                y >= this.minY && y < this.maxY &&
                z >= this.minZ && z < this.maxZ;
    }

    @Override
    public void set(int x, int y, int z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMin(Direction.Axis axis) {
        return axis.choose(this.minX, this.minY, this.minZ);
    }

    @Override
    public int getMax(Direction.Axis axis) {
        return axis.choose(this.maxX, this.maxY, this.maxZ);
    }

    @Override
    public boolean isEmpty() {
        return this.minX >= this.maxX || this.minY >= this.maxY || this.minZ >= this.maxZ;
    }

}
