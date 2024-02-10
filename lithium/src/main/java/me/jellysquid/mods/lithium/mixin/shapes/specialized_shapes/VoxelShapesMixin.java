package me.jellysquid.mods.lithium.mixin.shapes.specialized_shapes;

import me.jellysquid.mods.lithium.common.shapes.VoxelShapeAlignedCuboid;
import me.jellysquid.mods.lithium.common.shapes.VoxelShapeEmpty;
import me.jellysquid.mods.lithium.common.shapes.VoxelShapeSimpleCube;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.spongepowered.asm.mixin.*;

/**
 * Shape specialization allows us to optimize comparison logic by guaranteeing certain constraints about the
 * configuration of vertices in a given shape. For example, most block shapes consist of only one cuboid and by
 * nature, only one voxel. This fact can be taken advantage of to create an optimized implementation which avoids
 * scanning over voxels as there are only ever two given vertices in the shape, allowing simple math operations to be
 * used for determining intersection and penetration.
 * <p>
 * In most cases, comparison logic is rather simple as the game often only deals with empty shapes or simple cubes.
 * Specialization provides a significant speed-up to entity collision resolution and various other parts of the game
 * without needing invasive patches, as we can simply replace the types returned by this class. Modern processors
 * (along with the help of the potent JVM) make the cost of dynamic dispatch negligible when compared to the execution
 * times of shape comparison methods.
 */
@Mixin(VoxelShapes.class)
public abstract class VoxelShapesMixin {
    @Mutable
    @Shadow
    @Final
    public static final VoxelShape UNBOUNDED;

    @Mutable
    @Shadow
    @Final
    private static final VoxelShape FULL_CUBE;

    @Mutable
    @Shadow
    @Final
    private static final VoxelShape EMPTY;

    private static final VoxelSet FULL_CUBE_VOXELS;

    // Re-initialize the global cached shapes with our specialized ones. This will happen right after all the static
    // state has been initialized and before any external classes access it.
    static {
        // [VanillaCopy] The FULL_CUBE and UNBOUNDED shape is initialized with a single 1x1x1 voxel as neither will
        // contain multiple inner cuboids.
        FULL_CUBE_VOXELS = new BitSetVoxelSet(1, 1, 1);
        FULL_CUBE_VOXELS.set(0, 0, 0);

        // Used in some rare cases to indicate a shape which encompasses the entire world (such as a moving world border)
        UNBOUNDED = new VoxelShapeSimpleCube(FULL_CUBE_VOXELS, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

        // Represents a full-block cube shape, such as that for a dirt block.
        FULL_CUBE = new VoxelShapeSimpleCube(FULL_CUBE_VOXELS, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

        // Represents an empty cube shape with no vertices that cannot be collided with.
        EMPTY = new VoxelShapeEmpty(new BitSetVoxelSet(0, 0, 0));
    }

    /**
     * Vanilla implements some very complex logic in this function in order to allow entity boxes to be used in
     * collision resolution the same way as block shapes. The specialized simple cube shape however can trivially
     * represent these cases with nothing more than the two vertexes. This provides a modest speed up for entity
     * collision code by allowing them to also use our optimized shapes.
     * <p>
     * Vanilla uses different kinds of VoxelShapes depending on the size and position of the box.
     * A box that isn't aligned with 1/8th of a block will become a very simple ArrayVoxelShape, while others
     * will become a "SimpleVoxelShape" with a BitSetVoxelSet that possibly has a higher resolution (1-3 bits) per axis.
     * <p>
     * Shapes that have a high resolution (e.g. extended piston base has 2 bits on one axis) have collision
     * layers inside them. An upwards extended piston base has extra collision boxes at 0.25 and 0.5 height.
     * Slabs don't have extra collision boxes, because they are only as high as the smallest height that is possible
     * with their bit resolution (1, so half a block).
     *
     * @reason Use our optimized shape types
     * @author JellySquid, 2No2Name
     */
    @Overwrite
    public static VoxelShape cuboidUnchecked(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (maxX - minX < 1.0E-7D || maxY - minY < 1.0E-7D || maxZ - minZ < 1.0E-7D) {
            return EMPTY;
        }

        int xRes;
        int yRes;
        int zRes;
        //findRequiredBitResolution(...) looks unnecessarily slow, and it seems to unintentionally return -1 on inputs like -1e-8,
        //A faster implementation is not in the scope of this mixin.

        //Description of what vanilla does:
        //If the VoxelShape cannot be represented by a BitSet with 3 bit resolution on any axis (BitSetVoxelSet),
        //a shape without boxes inside will be used in vanilla (ArrayVoxelShape with only 2 PointPositions on each axis)

        if ((xRes = VoxelShapes.findRequiredBitResolution(minX, maxX)) < 0 ||
                (yRes = VoxelShapes.findRequiredBitResolution(minY, maxY)) < 0 ||
                (zRes = VoxelShapes.findRequiredBitResolution(minZ, maxZ)) < 0) {
            //vanilla uses ArrayVoxelShape here without any rounding of the coordinates
            return new VoxelShapeSimpleCube(FULL_CUBE_VOXELS, minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            if (xRes == 0 && yRes == 0 && zRes == 0) {
                return FULL_CUBE;
            }
            // vanilla would use a SimpleVoxelShape with a BitSetVoxelSet of resolution of xRes, yRes, zRes here, we match its behavior
            return new VoxelShapeAlignedCuboid(Math.round(minX * 8D) / 8D, Math.round(minY * 8D) / 8D, Math.round(minZ * 8D) / 8D,
                    Math.round(maxX * 8D) / 8D, Math.round(maxY * 8D) / 8D, Math.round(maxZ * 8D) / 8D, xRes, yRes, zRes);
        }
    }
}
