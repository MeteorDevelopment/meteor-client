package me.jellysquid.mods.lithium.common.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.util.math.Direction.Axis.*;

public class VoxelShapeMatchesAnywhere {

    public static void cuboidMatchesAnywhere(VoxelShape shapeA, VoxelShape shapeB, BooleanBiFunction predicate, CallbackInfoReturnable<Boolean> cir) {
        //calling this method only if both shapes are not empty and have bounding box overlap

        if (shapeA instanceof VoxelShapeSimpleCube && shapeB instanceof VoxelShapeSimpleCube) {
            if (((VoxelShapeSimpleCube) shapeA).isTiny || ((VoxelShapeSimpleCube) shapeB).isTiny) {
                //vanilla fallback: Handling this special case would mean using the whole
                //pointPosition merging code in SimplePairList. A tiny shape can have very odd effects caused by
                //having three pointPositions within 2e-7 or even 1e-7. Vanilla merges the point positions
                //by always taking the most negative one and skipping those with less than 1e-7 distance to it.
                //The optimization partially relies on only having to check the previous point position, which is
                //not possible when 3 or more are within 2e-7 of another, as the previous point position could have
                //been skipped by the merging code.
                return;
            }
            //both shapes are simple cubes, matching two cubes anywhere is really simple. Also handle epsilon margins.
            if (predicate.apply(true, true)) {
                if (intersects((VoxelShapeSimpleCube) shapeA, (VoxelShapeSimpleCube) shapeB)) {
                    cir.setReturnValue(true);
                    return;
                }
                cir.setReturnValue(predicate.apply(true, false) || predicate.apply(false, true));
            } else if (predicate.apply(true, false) &&
                    exceedsShape((VoxelShapeSimpleCube) shapeA, (VoxelShapeSimpleCube) shapeB)) {
                cir.setReturnValue(true);
                return;
            } else if (predicate.apply(false, true) &&
                    exceedsShape((VoxelShapeSimpleCube) shapeB, (VoxelShapeSimpleCube) shapeA)) {
                cir.setReturnValue(true);
                return;
            }
            cir.setReturnValue(false);
        }
        else if (shapeA instanceof VoxelShapeSimpleCube || shapeB instanceof VoxelShapeSimpleCube) {
            //only one of the two shapes is a simple cube, but there are still some shortcuts that can be taken
            VoxelShapeSimpleCube simpleCube = (VoxelShapeSimpleCube) (shapeA instanceof VoxelShapeSimpleCube ? shapeA : shapeB);
            VoxelShape otherShape = simpleCube == shapeA ? shapeB : shapeA;

            if (simpleCube.isTiny || isTiny(otherShape)) {
                //vanilla fallback, same reason as above
                return;
            }

            boolean acceptSimpleCubeAlone = predicate.apply(shapeA == simpleCube, shapeB == simpleCube);
            //test the area outside otherShape
            if (acceptSimpleCubeAlone && exceedsCube(simpleCube,
                    otherShape.getMin(X), otherShape.getMin(Y), otherShape.getMin(Z),
                    otherShape.getMax(X), otherShape.getMax(Y), otherShape.getMax(Z))) {
                cir.setReturnValue(true);
                return;
            }
            boolean acceptAnd = predicate.apply(true, true);
            boolean acceptOtherShapeAlone = predicate.apply(shapeA == otherShape, shapeB == otherShape);

            //test the area inside otherShape
            VoxelSet voxelSet = otherShape.voxels;
            DoubleList pointPositionsX = otherShape.getPointPositions(X);
            DoubleList pointPositionsY = otherShape.getPointPositions(Y);
            DoubleList pointPositionsZ = otherShape.getPointPositions(Z);

            int xMax = voxelSet.getMax(X); // xMax <= pointPositionsX.size()
            int yMax = voxelSet.getMax(Y);
            int zMax = voxelSet.getMax(Z);

            //keep the cube positions in local vars to avoid looking them up all the time
            double simpleCubeMaxX = simpleCube.getMax(X);
            double simpleCubeMinX = simpleCube.getMin(X);
            double simpleCubeMaxY = simpleCube.getMax(Y);
            double simpleCubeMinY = simpleCube.getMin(Y);
            double simpleCubeMaxZ = simpleCube.getMax(Z);
            double simpleCubeMinZ = simpleCube.getMin(Z);

            //iterate over all entries of the VoxelSet
            for (int x = voxelSet.getMin(X); x < xMax; x++) {
                //all of the positions of +1e-7 and -1e-7 and >, >=, <, <= are carefully chosen:
                //for example for the following line:                       >= here fails the test
                //                                        moving the - 1e-7 here to the other side of > as + 1e-7 fails the test
                boolean simpleCubeIntersectsXSlice = (simpleCubeMaxX - 1e-7 > pointPositionsX.getDouble(x) && simpleCubeMinX < pointPositionsX.getDouble(x + 1) - 1e-7);
                if (!acceptOtherShapeAlone && !simpleCubeIntersectsXSlice) {
                    //if we cannot return when the simple cube is not intersecting the area, skip forward
                    continue;
                }
                boolean xSliceExceedsCube = acceptOtherShapeAlone && !((simpleCubeMaxX >= pointPositionsX.getDouble(x + 1) - 1e-7 && simpleCubeMinX - 1e-7 <= pointPositionsX.getDouble(x)));
                for (int y = voxelSet.getMin(Y); y < yMax; y++) {
                    boolean simpleCubeIntersectsYSlice = (simpleCubeMaxY - 1e-7 > pointPositionsY.getDouble(y) && simpleCubeMinY < pointPositionsY.getDouble(y + 1) - 1e-7);
                    if (!acceptOtherShapeAlone && !simpleCubeIntersectsYSlice) {
                        //if we cannot return when the simple cube is not intersecting the area, skip forward
                        continue;
                    }
                    boolean ySliceExceedsCube = acceptOtherShapeAlone && !((simpleCubeMaxY >= pointPositionsY.getDouble(y + 1) - 1e-7 && simpleCubeMinY - 1e-7 <= pointPositionsY.getDouble(y)));
                    for (int z = voxelSet.getMin(Z); z < zMax; z++) {
                        boolean simpleCubeIntersectsZSlice = (simpleCubeMaxZ - 1e-7 > pointPositionsZ.getDouble(z) && simpleCubeMinZ < pointPositionsZ.getDouble(z + 1) - 1e-7);
                        if (!acceptOtherShapeAlone && !simpleCubeIntersectsZSlice) {
                            //if we cannot return when the simple cube is not intersecting the area, skip forward
                            continue;
                        }
                        boolean zSliceExceedsCube = acceptOtherShapeAlone && !((simpleCubeMaxZ >= pointPositionsZ.getDouble(z + 1) - 1e-7 && simpleCubeMinZ - 1e-7 <= pointPositionsZ.getDouble(z)));

                        boolean o = voxelSet.inBoundsAndContains(x, y, z);
                        boolean s = simpleCubeIntersectsXSlice && simpleCubeIntersectsYSlice && simpleCubeIntersectsZSlice;
                        if (acceptAnd && o && s || acceptSimpleCubeAlone && !o && s || acceptOtherShapeAlone && o && (xSliceExceedsCube || ySliceExceedsCube || zSliceExceedsCube)) {
                            cir.setReturnValue(true);
                            return;
                        }
                    }
                }
            }
            cir.setReturnValue(false);
        }
    }

    private static boolean isTiny(VoxelShape shapeA) {
        //avoid properties of SimplePairList, really close point positions are subject to special merging behavior
        return shapeA.getMin(X) > shapeA.getMax(X) - 3e-7 ||
                shapeA.getMin(Y) > shapeA.getMax(Y) - 3e-7 ||
                shapeA.getMin(Z) > shapeA.getMax(Z) - 3e-7;
    }

    private static boolean exceedsCube(VoxelShapeSimpleCube a, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return a.getMin(X) < minX - 1e-7 || a.getMax(X) > maxX + 1e-7 ||
                a.getMin(Y) < minY - 1e-7 || a.getMax(Y) > maxY + 1e-7 ||
                a.getMin(Z) < minZ - 1e-7 || a.getMax(Z) > maxZ + 1e-7;
    }

    private static boolean exceedsShape(VoxelShapeSimpleCube a, VoxelShapeSimpleCube b) {
        return a.getMin(X) < b.getMin(X) - 1e-7 || a.getMax(X) > b.getMax(X) + 1e-7 ||
                a.getMin(Y) < b.getMin(Y) - 1e-7 || a.getMax(Y) > b.getMax(Y) + 1e-7 ||
                a.getMin(Z) < b.getMin(Z) - 1e-7 || a.getMax(Z) > b.getMax(Z) + 1e-7;
    }

    private static boolean intersects(VoxelShapeSimpleCube a, VoxelShapeSimpleCube b) {
        return a.getMin(X) < b.getMax(X) - 1e-7 && a.getMax(X) > b.getMin(X) + 1e-7 &&
                a.getMin(Y) < b.getMax(Y) - 1e-7 && a.getMax(Y) > b.getMin(Y) + 1e-7 &&
                a.getMin(Z) < b.getMax(Z) - 1e-7 && a.getMax(Z) > b.getMin(Z) + 1e-7;
    }
}