package net.minecraft.util.shape;

import me.jellysquid.mods.lithium.common.shapes.VoxelShapeAlignedCuboid;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.Random;

import static me.jellysquid.mods.lithium.mixin.shapes.specialized_shapes.VoxelShapesMixin.cuboidUnchecked;

/**
 * Test for the specialized shapes / custom voxel shape implementation.
 * This test compares the collision behavior of VoxelShapes using randomized boxes and randomized movements.
 * As 1/8 block aligned shapes, deviations by ~1e-7 and offset shapes are handled specially in vanilla code,
 * we attempt to also include those cases in the test.
 * The test prints its random seed when it fails, so failures are reproducible in the debugger.
 *
 * @author 2No2Name
 */
public class TestCustomVoxelShapesCollisions {
    public static void main(String[] args) {
        new TestCustomVoxelShapesCollisions().testCollisions();
        System.out.println("VoxelShape collision test passed.");
    }

    public static Direction.Axis[] AXES = Direction.Axis.values();

    public long noCollision;
    public long collision;
    public long intersects;
    public long intersectsCollision;
    public long withCollisionBoxesInside;
    public long randomSeed;


    public Random rand;
    public double[] distances;
    private Box[] boxes;

//    @Test
    public void testCollisions() {
        this.noCollision = 0;
        this.collision = 0;
        this.intersects = 0;
        this.intersectsCollision = 0;
        this.withCollisionBoxesInside = 0;

        this.rand = new Random();
        //insert your seed here for debugging when the test failed and printed its seed
        long seed = this.rand.nextLong();
        this.rand.setSeed(seed);
        this.randomSeed = seed;

        this.boxes = new Box[20];
        this.boxes[0] = new Box(0.2931994021407849, 0.5175531777466607, 0.14575020167685837, 1.4562191976519117, 2.2389429614133496, 0.31827209851790766);
        this.boxes[1] = new Box(0.5 - 0.5E-7, 0.75, 0.125 + 1E-7, 1, 1+1E-6, 0.5);
        for (int i = 2; i < 20; i++) {
            this.boxes[i] = getRandomBox(this.rand);
        }

        Random rand = this.rand;
        this.distances = new double[]{2.814955055053852, 5 * rand.nextDouble(), -5 * rand.nextDouble(), 5 * rand.nextDouble(), -5 * rand.nextDouble(), 1E-7, 1.1E-7, 0.9E-7, -1E-7, -1.1E-7, -0.9E-7, 1, 10, -1, -10, 0.1, -0.1, 0.25, -0.25, 0.33, -0.33, 1 - 1E-7, -1 + 1E-7, 5 * rand.nextDouble(), -5 * rand.nextDouble(), 5 * rand.nextDouble(), -5 * rand.nextDouble()};


        //test all of the 1/8th of a block aligned shapes
        for (int x = 0; x <= 7; x++) {
            for(int x2 = x + 1; x2 <= 8; x2++) {
                for (int y = 0; y <= 7; y++) {
                    for(int y2 = y + 1; y2 <= 8; y2++) {
                        for (int z = 0; z <= 7; z++) {
                            for(int z2 = z + 1; z2 <= 8; z2++) {
                                VoxelShape[] pair = getVanillaModdedVoxelShapePair(new Box(x/8D,y/8D,z/8D,x2/8D,y2/8D,z2/8D));
                                this.testShapeBehaviorEquality(pair);
                                //test random offsetting to test VoxelShapeAlignedCuboid_Offset
                                double xOff = 5* rand.nextGaussian();
                                double yOff = 4* rand.nextDouble();
                                double zOff = rand.nextDouble();
                                pair[0] = pair[0].offset(xOff, yOff, zOff);
                                pair[1] = pair[1].offset(xOff, yOff, zOff);
                                this.testShapeBehaviorEquality(pair);
                                //test random EPSILON-sized deviations
                                pair = getVanillaModdedVoxelShapePair(new Box(x/8D + 2E-7*this.rand.nextDouble(),y/8D + 4E-8,z/8D,x2/8D,y2/8D+9E-8,z2/8D));
                                this.testShapeBehaviorEquality(pair);
                            }
                        }
                    }
                }
            }
        }
        //test some random shapes, just in case there is a really stupid mistake somewhere
        for (int i = 0; i < 2000; i++) {
            this.testShapeBehaviorEquality(getVanillaModdedVoxelShapePair(new Box(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian())));
        }
    }

    public static Box getRandomBox(Random random) {
        double x1 = random.nextDouble() * 2;
        double y1 = random.nextDouble() * 2;
        double z1 = random.nextDouble() * 2;
        return new Box(x1, y1, z1, 2*x1 + random.nextGaussian(), 2*y1 + random.nextGaussian(), 2*z1 * random.nextGaussian());
    }

    public void testShapeBehaviorEquality(VoxelShape[] pair) {
        for (Direction.Axis axis : AXES) {
            for (double maxDist : this.distances) {
                for (Box box : this.boxes) {
                    double resultVanilla = pair[0].calculateMaxDistance(axis, box, maxDist);
                    double resultModded = pair[1].calculateMaxDistance(axis, box, maxDist);
                    int collided = 0;
                    if (resultVanilla == maxDist) {
                        this.noCollision++;
                    } else {
                        this.collision++;
                        collided = 1;
                    }
                    if (pair[0].getBoundingBox().intersects(box)) {
                        this.intersects++;
                        this.intersectsCollision += collided;
                    }
                    if (pair[1] instanceof VoxelShapeAlignedCuboid) {
                        this.withCollisionBoxesInside++;
                    }

                    if (resultModded != resultVanilla) {
//these lines only make debugging easier
//                        boolean repeat = true;
//                        while(repeat) {
//                            double resultVanilla2 = pair[0].calculateMaxDistance(axis, box, maxDist);
//                            double resultModded2 = pair[1].calculateMaxDistance(axis, box, maxDist);
//                            if (resultVanilla != resultVanilla2)
//                                repeat = false;
//                        }
                        throw new IllegalStateException(String.format("RNG seed: %s, different results for: %s, %s in calculateMaxDistance with arguments axis: %s, box: %s, maxDist: %s, result vanilla: %s, result modded: %s", this.randomSeed, pair[0], pair[1], axis, box, maxDist, resultVanilla, resultModded));
                    }
                }
            }
        }
    }

    public static VoxelShape[] getVanillaModdedVoxelShapePair(Box box) {
        return new VoxelShape[]{VoxelShapes.cuboid(box), cuboid(box)};
    }

    private static VoxelShape cuboid(Box box) {
        return cuboidUnchecked(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }
}
