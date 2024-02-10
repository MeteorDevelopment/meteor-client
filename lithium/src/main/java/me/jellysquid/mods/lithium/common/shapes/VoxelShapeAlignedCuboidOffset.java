package me.jellysquid.mods.lithium.common.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import me.jellysquid.mods.lithium.common.shapes.lists.OffsetFractionalDoubleList;
import net.minecraft.util.math.AxisCycleDirection;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;

public class VoxelShapeAlignedCuboidOffset extends VoxelShapeAlignedCuboid {
    //keep track on how much the voxelSet was offset. minX,maxX,minY are stored offset already
    //This is only required to calculate the position of the walls inside the VoxelShape.
    //For shapes that are not offset the alignment is 0, but for offset shapes the walls move together with the shape.
    private final double xOffset, yOffset, zOffset;
    //instead of keeping those variables, equivalent information can probably be recovered from minX, minY, minZ (which are 1/8th of a block aligned), but possibly with additional floating point error

    public VoxelShapeAlignedCuboidOffset(VoxelShapeAlignedCuboid originalShape, VoxelSet voxels, double xOffset, double yOffset, double zOffset) {
        super(voxels, originalShape.xSegments, originalShape.ySegments, originalShape.zSegments,
                originalShape.minX + xOffset, originalShape.minY + yOffset, originalShape.minZ + zOffset,
                originalShape.maxX + xOffset, originalShape.maxY + yOffset, originalShape.maxZ + zOffset);

        if (originalShape instanceof VoxelShapeAlignedCuboidOffset) {
            this.xOffset = ((VoxelShapeAlignedCuboidOffset) originalShape).xOffset + xOffset;
            this.yOffset = ((VoxelShapeAlignedCuboidOffset) originalShape).yOffset + yOffset;
            this.zOffset = ((VoxelShapeAlignedCuboidOffset) originalShape).zOffset + zOffset;
        } else {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.zOffset = zOffset;
        }
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
                return VoxelShapeAlignedCuboidOffset.calculatePenetration(this.minX, this.maxX, this.xSegments, this.xOffset, box.minX, box.maxX, maxDist);
            case FORWARD:
                return VoxelShapeAlignedCuboidOffset.calculatePenetration(this.minZ, this.maxZ, this.zSegments, this.zOffset, box.minZ, box.maxZ, maxDist);
            case BACKWARD:
                return VoxelShapeAlignedCuboidOffset.calculatePenetration(this.minY, this.maxY, this.ySegments, this.yOffset, box.minY, box.maxY, maxDist);
            default:
                throw new IllegalArgumentException();
        }
    }


    /**
     * Determine how far the movement is possible.
     */
    private static double calculatePenetration(double aMin, double aMax, final int segmentsPerUnit, double shapeOffset, double bMin, double bMax, double maxDist) {
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
                //extra segment walls / hitboxes inside this shape, evenly spaced out in 0..1 + shapeOffset
                //round to the next segment wall, but with epsilon margin like vanilla

                //using large epsilon and extra check here because +- shapeOffset can cause larger floating point errors
                int segment = MathHelper.ceil((bMax - LARGE_EPSILON - shapeOffset) * segmentsPerUnit);
                double wallPos = segment / (double) segmentsPerUnit + shapeOffset;
                if (wallPos < bMax - EPSILON) {
                    ++segment;
                    wallPos = segment / (double) segmentsPerUnit + shapeOffset;
                }
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

                //using large epsilon and extra check here because +- shapeOffset can cause larger floating point errors
                int segment = MathHelper.floor((bMin + LARGE_EPSILON - shapeOffset) * segmentsPerUnit);
                double wallPos = segment / (double) segmentsPerUnit + shapeOffset;
                if (wallPos > bMin + EPSILON) {
                    --segment;
                    wallPos = segment / (double) segmentsPerUnit + shapeOffset;
                }
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
        return new OffsetFractionalDoubleList(axis.choose(this.xSegments, this.ySegments, this.zSegments),
                axis.choose(this.xOffset, this.yOffset, this.zOffset));
    }

    @Override
    protected double getPointPosition(Direction.Axis axis, int index) {
        return axis.choose(this.xOffset, this.yOffset, this.zOffset) +
                ((double) index / (double) axis.choose(this.xSegments, this.ySegments, this.zSegments));
    }

    @Override
    protected int getCoordIndex(Direction.Axis axis, double coord) {
        coord -= axis.choose(this.xOffset, this.yOffset, this.zOffset);
        int numSegments = axis.choose(this.xSegments, this.ySegments, this.zSegments);
        return MathHelper.clamp(MathHelper.floor(coord * (double) numSegments), -1, numSegments);
    }
}
