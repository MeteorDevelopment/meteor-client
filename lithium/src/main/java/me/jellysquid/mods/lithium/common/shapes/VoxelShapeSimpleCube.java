package me.jellysquid.mods.lithium.common.shapes;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.math.AxisCycleDirection;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.List;

/**
 * An efficient implementation of {@link VoxelShape} for a shape with one simple cuboid. Since there are only ever two
 * vertices in a single cuboid (the start and end points), we can eliminate needing to iterate over voxels and to find
 * vertices through using simple comparison logic to pick between either the start or end point.
 * <p>
 * Additionally, the function responsible for determining shape penetration has been simplified and optimized by taking
 * advantage of the fact that there is only ever one voxel in a simple cuboid shape, greatly speeding up collision
 * handling in most cases as block shapes are often nothing more than a single cuboid.
 */
public class VoxelShapeSimpleCube extends VoxelShape implements VoxelShapeCaster {
    static final double EPSILON = 1.0E-7D;

    final double minX, minY, minZ, maxX, maxY, maxZ;
    public final boolean isTiny;

    public VoxelShapeSimpleCube(VoxelSet voxels, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        super(voxels);

        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;

        this.isTiny =
                this.minX + 3 * EPSILON >= this.maxX ||
                        this.minY + 3 * EPSILON >= this.maxY ||
                        this.minZ + 3 * EPSILON >= this.maxZ;
    }

    @Override
    public VoxelShape offset(double x, double y, double z) {
        return new VoxelShapeSimpleCube(this.voxels, this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    @Override
    public double calculateMaxDistance(AxisCycleDirection cycleDirection, Box box, double maxDist) {
        if (Math.abs(maxDist) < EPSILON) {
            return 0.0D;
        }

        double penetration = this.calculatePenetration(cycleDirection, box, maxDist);

        if ((penetration != maxDist) && this.intersects(cycleDirection, box)) {
            return penetration;
        }

        return maxDist;
    }

    private double calculatePenetration(AxisCycleDirection dir, Box box, double maxDist) {
        switch (dir) {
            case NONE:
                return VoxelShapeSimpleCube.calculatePenetration(this.minX, this.maxX, box.minX, box.maxX, maxDist);
            case FORWARD:
                return VoxelShapeSimpleCube.calculatePenetration(this.minZ, this.maxZ, box.minZ, box.maxZ, maxDist);
            case BACKWARD:
                return VoxelShapeSimpleCube.calculatePenetration(this.minY, this.maxY, box.minY, box.maxY, maxDist);
            default:
                throw new IllegalArgumentException();
        }
    }

    boolean intersects(AxisCycleDirection dir, Box box) {
        switch (dir) {
            case NONE:
                return lessThan(this.minY, box.maxY) && lessThan(box.minY, this.maxY) && lessThan(this.minZ, box.maxZ) && lessThan(box.minZ, this.maxZ);
            case FORWARD:
                return lessThan(this.minX, box.maxX) && lessThan(box.minX, this.maxX) && lessThan(this.minY, box.maxY) && lessThan(box.minY, this.maxY);
            case BACKWARD:
                return lessThan(this.minZ, box.maxZ) && lessThan(box.minZ, this.maxZ) && lessThan(this.minX, box.maxX) && lessThan(box.minX, this.maxX);
            default:
                throw new IllegalArgumentException();
        }
    }

    private static double calculatePenetration(double a1, double a2, double b1, double b2, double maxDist) {
        double penetration;

        if (maxDist > 0.0D) {
            penetration = a1 - b2;

            if ((penetration < -EPSILON) || (maxDist < penetration)) {
                //already far enough inside this shape to not collide with the surface or
                //outside the shape and still far enough away for no collision at all
                return maxDist;
            }
            //allow moving up to the shape but not into it. This also includes going backwards by at most EPSILON.
        } else {
            //whole code again, just negated for the other direction
            penetration = a2 - b1;

            if ((penetration > EPSILON) || (maxDist > penetration)) {
                return maxDist;
            }
        }

        return penetration;
    }

    @Override
    public List<Box> getBoundingBoxes() {
        return Lists.newArrayList(this.getBoundingBox());
    }

    @Override
    public Box getBoundingBox() {
        return new Box(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    @Override
    public double getMin(Direction.Axis axis) {
        return axis.choose(this.minX, this.minY, this.minZ);
    }

    @Override
    public double getMax(Direction.Axis axis) {
        return axis.choose(this.maxX, this.maxY, this.maxZ);
    }

    @Override
    protected double getPointPosition(Direction.Axis axis, int index) {
        if ((index < 0) || (index > 1)) {
            throw new ArrayIndexOutOfBoundsException();
        }

        switch (axis) {
            case X:
                return (index == 0) ? this.minX : this.maxX;
            case Y:
                return (index == 0) ? this.minY : this.maxY;
            case Z:
                return (index == 0) ? this.minZ : this.maxZ;
        }

        throw new IllegalArgumentException();
    }

    @Override
    public DoubleList getPointPositions(Direction.Axis axis) {
        switch (axis) {
            case X:
                return DoubleArrayList.wrap(new double[]{this.minX, this.maxX});
            case Y:
                return DoubleArrayList.wrap(new double[]{this.minY, this.maxY});
            case Z:
                return DoubleArrayList.wrap(new double[]{this.minZ, this.maxZ});
        }

        throw new IllegalArgumentException();
    }


    @Override
    public boolean isEmpty() {
        return (this.minX >= this.maxX) || (this.minY >= this.maxY) || (this.minZ >= this.maxZ);
    }

    @Override
    protected int getCoordIndex(Direction.Axis axis, double coord) {
        if (coord < this.getMin(axis)) {
            return -1;
        }

        if (coord >= this.getMax(axis)) {
            return 1;
        }

        return 0;
    }

    private static boolean lessThan(double a, double b) {
        return (a + EPSILON) < b;
    }

    @Override
    public boolean intersects(Box box, double blockX, double blockY, double blockZ) {
        return ((box.minX + 1e-7) < (this.maxX + blockX)) && ((box.maxX - 1e-7) > (this.minX + blockX)) &&
                ((box.minY + 1e-7) < (this.maxY + blockY)) && ((box.maxY - 1e-7) > (this.minY + blockY)) &&
                ((box.minZ + 1e-7) < (this.maxZ + blockZ)) && ((box.maxZ - 1e-7) > (this.minZ + blockZ));
    }


    @Override
    public void forEachBox(VoxelShapes.BoxConsumer boxConsumer) {
        boxConsumer.consume(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }
}
