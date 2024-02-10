package me.jellysquid.mods.lithium.common.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.math.AxisCycleDirection;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.FractionalDoubleList;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;

/**
 * An efficient implementation of {@link VoxelShape} for a shape with one simple cuboid.
 * This is an alternative to VoxelShapeSimpleCube with extra hitboxes inside.
 * Vanilla has extra hitboxes at steps of 1/8th or 1/4th of a block depending on the exact coordinates of the shape.
 * We are mimicking the effect on collisions here, as otherwise some contraptions would not behave like vanilla.
 *
 * @author 2No2Name
 */
public class VoxelShapeAlignedCuboid extends VoxelShapeSimpleCube {
    //EPSILON for use in cases where it must be a lot smaller than 1/256 and larger than EPSILON
    static final double LARGE_EPSILON = 10 * EPSILON;

    //In bit aligned shapes the bitset adds segments are between minX/Y/Z and maxX/Y/Z.
    //Segments all have the same size. There is an additional collision box between two adjacent segments (if both are inside the shape)
    protected final int xSegments;
    protected final int ySegments;
    protected final int zSegments;

    public VoxelShapeAlignedCuboid(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int xRes, int yRes, int zRes) {
        super(new CuboidVoxelSet(1 << xRes, 1 << yRes, 1 << zRes, minX, minY, minZ, maxX, maxY, maxZ), minX, minY, minZ, maxX, maxY, maxZ);

        this.xSegments = 1 << xRes;
        this.ySegments = 1 << yRes;
        this.zSegments = 1 << zRes;
    }

    /**
     * Constructor for use in offset() calls.
     */
    public VoxelShapeAlignedCuboid(VoxelSet voxels, int xSegments, int ySegments, int zSegments, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        super(voxels, minX, minY, minZ, maxX, maxY, maxZ);

        this.xSegments = xSegments;
        this.ySegments = ySegments;
        this.zSegments = zSegments;
    }

    @Override
    public VoxelShape offset(double x, double y, double z) {
        return new VoxelShapeAlignedCuboidOffset(this, this.voxels, x, y, z);
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
                return VoxelShapeAlignedCuboid.calculatePenetration(this.minX, this.maxX, this.xSegments, box.minX, box.maxX, maxDist);
            case FORWARD:
                return VoxelShapeAlignedCuboid.calculatePenetration(this.minZ, this.maxZ, this.zSegments, box.minZ, box.maxZ, maxDist);
            case BACKWARD:
                return VoxelShapeAlignedCuboid.calculatePenetration(this.minY, this.maxY, this.ySegments, box.minY, box.maxY, maxDist);
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Determine how far the movement is possible.
     */
    private static double calculatePenetration(double aMin, double aMax, final int segmentsPerUnit, double bMin, double bMax, double maxDist) {
        double gap;

        if (maxDist > 0.0D) {
            gap = aMin - bMax;

            if (gap >= -EPSILON) {
                //outside the shape/within margin, move up to/back to boundary
                return Math.min(gap, maxDist);
            } else {
                //already far enough inside this shape to not collide with the surface
                if (segmentsPerUnit == 1) {
                    //no extra segments to collide with, because only one segment in total
                    return maxDist;
                }
                //extra segment walls / hitboxes inside this shape, evenly spaced out in 0..1
                //round to the next segment wall, but with epsilon margin like vanilla
                double wallPos = MathHelper.ceil((bMax - EPSILON) * segmentsPerUnit) / (double) segmentsPerUnit;
                //only use the wall when it is actually inside the shape, and not a border / outside the shape
                if (wallPos < aMax - LARGE_EPSILON) {
                    return Math.min(maxDist, wallPos - bMax);
                }
                return maxDist;
            }
        } else {
            //whole code again, just negated for the other direction
            gap = aMax - bMin;

            if (gap <= EPSILON) {
                //outside the shape/within margin, move up to/back to boundary
                return Math.max(gap, maxDist);
            } else {
                //already far enough inside this shape to not collide with the surface
                if (segmentsPerUnit == 1) {
                    //no extra segments to collide with, because only one segment in total
                    return maxDist;
                }
                //extra segment walls / hitboxes inside this shape, evenly spaced out in 0..1
                //round to the next segment wall, but with epsilon margin like vanilla
                double wallPos = MathHelper.floor((bMin + EPSILON) * segmentsPerUnit) / (double) segmentsPerUnit;
                //only use the wall when it is actually inside the shape, and not a border / outside the shape
                if (wallPos > aMin + LARGE_EPSILON) {
                    return Math.max(maxDist, wallPos - bMin);
                }
                return maxDist;
            }
        }
    }

    @Override
    public DoubleList getPointPositions(Direction.Axis axis) {
        return new FractionalDoubleList(axis.choose(this.xSegments, this.ySegments, this.zSegments));
    }

    @Override
    protected double getPointPosition(Direction.Axis axis, int index) {
        return (double) index / (double) axis.choose(this.xSegments, this.ySegments, this.zSegments);
    }

    @Override
    protected int getCoordIndex(Direction.Axis axis, double coord) {
        int i = axis.choose(this.xSegments, this.ySegments, this.zSegments);
        return MathHelper.clamp(MathHelper.floor(coord * (double) i), -1, i);
    }
}
