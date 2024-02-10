package me.jellysquid.mods.lithium.mixin.shapes.optimized_matching;

import net.minecraft.block.Block;
import net.minecraft.util.Util;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.AxisCycleDirection;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

import static me.jellysquid.mods.lithium.common.shapes.VoxelShapeMatchesAnywhere.cuboidMatchesAnywhere;
import static me.jellysquid.mods.lithium.mixin.shapes.specialized_shapes.VoxelShapesMixin.cuboidUnchecked;
import static net.minecraft.block.Block.createCuboidShape;

/**
 * Test for the optimized shape matching implementation.
 * This test compares the behavior of the optimization to vanilla code.
 *
 * @author 2No2Name
 */
public class TestOptimizedVoxelShapeMatchesAnywhere {
    static final BooleanBiFunction[] FUNCTIONS = {BooleanBiFunction.AND, BooleanBiFunction.ONLY_FIRST, BooleanBiFunction.ONLY_SECOND, BooleanBiFunction.NOT_SAME};
    static final VoxelShape[] TESTED_COMPLEX_SHAPES = {
            //Cauldron:
            VoxelShapes.combineAndSimplify(VoxelShapes.fullCube(), VoxelShapes.union(createCuboidShape(0.0D, 0.0D, 4.0D, 16.0D, 3.0D, 12.0D), createCuboidShape(4.0D, 0.0D, 0.0D, 12.0D, 3.0D, 16.0D), createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D), createCuboidShape(2.0D, 4.0D, 2.0D, 14.0D, 16.0D, 14.0D)), BooleanBiFunction.ONLY_FIRST),
            //Hopper
            VoxelShapes.union(VoxelShapes.combineAndSimplify(VoxelShapes.union(Block.createCuboidShape(4.0D, 4.0D, 4.0D, 12.0D, 10.0D, 12.0D), Block.createCuboidShape(0.0D, 10.0D, 0.0D, 16.0D, 16.0D, 16.0D)), Block.createCuboidShape(2.0D, 11.0D, 2.0D, 14.0D, 16.0D, 14.0D), BooleanBiFunction.ONLY_FIRST), Block.createCuboidShape(12.0D, 4.0D, 6.0D, 16.0D, 8.0D, 10.0D)),
            //Anvil
            VoxelShapes.union(Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 4.0D, 14.0D), Block.createCuboidShape(3.0D, 4.0D, 4.0D, 13.0D, 5.0D, 12.0D), Block.createCuboidShape(4.0D, 5.0D, 6.0D, 12.0D, 10.0D, 10.0D), Block.createCuboidShape(0.0D, 10.0D, 3.0D, 16.0D, 16.0D, 13.0D)),
            //Bell on wall
            VoxelShapes.union(VoxelShapes.union(Block.createCuboidShape(4.0D, 4.0D, 4.0D, 12.0D, 6.0D, 12.0D), Block.createCuboidShape(5.0D, 6.0D, 5.0D, 11.0D, 13.0D, 11.0D)), Block.createCuboidShape(7.0D, 13.0D, 0.0D, 9.0D, 15.0D, 13.0D)),
            //Bell hanging
            VoxelShapes.union(VoxelShapes.union(Block.createCuboidShape(4.0D, 4.0D, 4.0D, 12.0D, 6.0D, 12.0D), Block.createCuboidShape(5.0D, 6.0D, 5.0D, 11.0D, 13.0D, 11.0D)), Block.createCuboidShape(7.0D, 13.0D, 7.0D, 9.0D, 16.0D, 9.0D))
    };

    public static void main(String[] args) {
        randomTestMatchesAnywhere();
        System.out.println("VoxelShape shape matching test passed.");
    }

    static int total = 0;
    static int matchedAnywhere = 0;
    static int notMatchedAnywhere = 0;
    static int notRunModCode = 0;

    private static void randomTestMatchesAnywhere() {
        total = 0;
        matchedAnywhere = 0;
        notMatchedAnywhere = 0;
        notRunModCode = 0;
        Random random = Random.createLocal();
        VoxelShapeVoxelShapePair pair = null;

        try {
            for (int i = 0; i < 100000000; i++) {
                //get two random cuboid shapes that are offset so often usually barely touch or barely not touch.
                pair = getRandomTest(random);
                testMatchesAnywhere(pair.a, pair.b, pair.function);
                //use one of the predefined complex non cuboid shapes and a random box randomly close to it
                pair = getRandomTestWithComplexShape(random);
                testMatchesAnywhere(pair.a, pair.b, pair.function);
            }
        } catch (Exception e) {
            if (pair == null) {
                throw new IllegalStateException("Test failed in initialization");
            }
            e.printStackTrace();
            throw new IllegalStateException("Test failed with args: " + pair.a + ", " + pair.b + ", " + Arrays.asList(FUNCTIONS).indexOf(pair.function));
        }
        System.out.println("Total: " + total + "\nSkippedModCode: " + notRunModCode + "\nMatchedAnywhere: " + matchedAnywhere + "\nNotMatchedAnywhere: " + notMatchedAnywhere);
    }

    private static void testMatchesAnywhere(VoxelShape one, VoxelShape two, BooleanBiFunction function) {
        //get the vanilla behavior to compare our implementation with
        boolean vanillaResult = VoxelShapes.matchesAnywhere(one, two, function);
        total++;
        if (vanillaResult) {
            matchedAnywhere++;
        } else {
            notMatchedAnywhere++;
        }

        int moddedResult = matchesAnywhereModded(one, two, function);
        if (moddedResult == -1) {
            notRunModCode++;
        }
        if ((moddedResult == 1) != vanillaResult && moddedResult != -1) {
            //these lines only make debugging easier
            boolean repeat = true;
            while (repeat) {
                int moddedResult2 = matchesAnywhereModded(one, two, function);
                boolean vanillaResult2 = VoxelShapes.matchesAnywhere(one, two, function);
                repeat = moddedResult2 != 10;
            }

            throw new IllegalStateException("Modded delivers unexpected result!");
        }
    }

    private static int matchesAnywhereModded(VoxelShape shape1, VoxelShape shape2, BooleanBiFunction predicate) {
        //code from vanilla
        if (predicate.apply(false, false)) {
            throw Util.throwOrPause(new IllegalArgumentException());
        } else if (shape1 == shape2) {
            return -1;
        } else if (shape1.isEmpty()) {
            return -1;
        } else if (shape2.isEmpty()) {
            return -1;
        } else {
            Direction.Axis[] var5 = AxisCycleDirection.AXES;
            int var6 = var5.length;

            for (int var7 = 0; var7 < var6; ++var7) {
                Direction.Axis axis = var5[var7];
                if (shape1.getMax(axis) < shape2.getMin(axis) - 1.0E-7D) {
                    return -1;
                }

                if (shape2.getMax(axis) < shape1.getMin(axis) - 1.0E-7D) {
                    return -1;
                }
            }
            //lithium code
            CallbackInfoReturnable<Boolean> cir = new CallbackInfoReturnable<>("matchesAnywhereModded", true);
            cuboidMatchesAnywhere(shape1, shape2, predicate, cir);
            if (cir.isCancelled()) {
                return cir.getReturnValueZ() ? 1 : 0;
            }
            return -1;
        }
    }


    public static VoxelShapeVoxelShapePair getRandomTest(Random random) {
        double x = random.nextInt(1000) - 500 + random.nextDouble();
        double y = random.nextInt(1000) - 500 + random.nextDouble();
        double z = random.nextInt(1000) - 500 + random.nextDouble();

        double xRadius = random.nextInt(3) + random.nextDouble() - 0.01;
        double yRadius = random.nextInt(3) + random.nextDouble() - 0.01;
        double zRadius = random.nextInt(3) + random.nextDouble() - 0.01;

        Box a = new Box(x - xRadius, y - yRadius, z - zRadius, x + xRadius, y + yRadius, z + zRadius);

        double xRadius2 = random.nextInt(3) + random.nextDouble() - 0.01;
        double yRadius2 = random.nextInt(3) + random.nextDouble() - 0.01;
        double zRadius2 = random.nextInt(3) + random.nextDouble() - 0.01;

        Direction touchSide = Direction.random(random);

        if (random.nextInt(8) > 0) {
            x += touchSide.getOffsetX() * (xRadius + xRadius2);
            y += touchSide.getOffsetY() * (yRadius + yRadius2);
            z += touchSide.getOffsetZ() * (zRadius + zRadius2);
        }

        x = getFuzzy(x, random, xRadius + xRadius2, 0.25f, 0.25f, 0.2f);
        y = getFuzzy(y, random, yRadius + yRadius2, 0.25f, 0.25f, 0.2f);
        z = getFuzzy(z, random, zRadius + zRadius2, 0.25f, 0.25f, 0.2f);

        Box b = new Box(x - xRadius2, y - yRadius2, z - zRadius2, x + xRadius2, y + yRadius2, z + zRadius2);

        BooleanBiFunction function = Util.getRandom(FUNCTIONS, random);

        if (random.nextBoolean()) {
            return new VoxelShapeVoxelShapePair(cuboid(b), cuboid(a), function);
        }
        return new VoxelShapeVoxelShapePair(cuboid(a), cuboid(b), function);
    }

    private static VoxelShape cuboid(Box box) {
        return cuboidUnchecked(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }


    private static VoxelShapeVoxelShapePair getRandomTestWithComplexShape(Random random) {
        VoxelShape complexShape = Util.getRandom(TESTED_COMPLEX_SHAPES, random);
        if (random.nextInt(8) > 1) {
            complexShape = complexShape.offset(
                    getFuzzy(random.nextInt(5) - 2, random, 0, 0.25f, 0.5f, 0f),
                    getFuzzy(random.nextInt(5) - 2, random, 0, 0.25f, 0.5f, 0f),
                    getFuzzy(random.nextInt(5) - 2, random, 0, 0.25f, 0.5f, 0f));
        }
        double x = 0.5D * (complexShape.getMin(Direction.Axis.X) + complexShape.getMax(Direction.Axis.X));
        double y = 0.5D * (complexShape.getMin(Direction.Axis.Y) + complexShape.getMax(Direction.Axis.Y));
        double z = 0.5D * (complexShape.getMin(Direction.Axis.Z) + complexShape.getMax(Direction.Axis.Z));

        double xRadius = 0.5D * (complexShape.getMax(Direction.Axis.X) - complexShape.getMin(Direction.Axis.X));
        double yRadius = 0.5D * (complexShape.getMax(Direction.Axis.Y) - complexShape.getMin(Direction.Axis.Y));
        double zRadius = 0.5D * (complexShape.getMax(Direction.Axis.Z) - complexShape.getMin(Direction.Axis.Z));

        double xRadius2 = random.nextInt(3) + random.nextDouble() - 0.01;
        double yRadius2 = random.nextInt(3) + random.nextDouble() - 0.01;
        double zRadius2 = random.nextInt(3) + random.nextDouble() - 0.01;

        Direction touchSide = Direction.random(random);

        if (random.nextInt(8) > 0) {
            x += touchSide.getOffsetX() * (xRadius + xRadius2);
            y += touchSide.getOffsetY() * (yRadius + yRadius2);
            z += touchSide.getOffsetZ() * (zRadius + zRadius2);
        }

        x = getFuzzy(x, random, xRadius + xRadius2, 0.25f, 0.25f, 0.25f);
        y = getFuzzy(y, random, yRadius + yRadius2, 0.25f, 0.25f, 0.25f);
        z = getFuzzy(z, random, zRadius + zRadius2, 0.25f, 0.25f, 0.25f);

        Box b = new Box(x - xRadius2, y - yRadius2, z - zRadius2, x + xRadius2, y + yRadius2, z + zRadius2);

        BooleanBiFunction function = Util.getRandom(FUNCTIONS, random);

        if (random.nextBoolean()) {
            return new VoxelShapeVoxelShapePair(complexShape, cuboid(b), function);
        }
        return new VoxelShapeVoxelShapePair(cuboid(b), complexShape, function);
    }

    private static double getFuzzy(double val, Random random, double specialValue, float chanceFuzz, float chanceBigFuzz, float chanceSpecialOffset) {
        if (random.nextFloat() < chanceFuzz) {
            if (random.nextInt(8) > 1) {
                val += 3e-7 * random.nextDouble() * (random.nextInt(2) * 2 - 1);
            } else {
                //test exactly around 1e-7 offsets
                val += (random.nextInt(3) - 1) * 1e-7;

                boolean b = random.nextBoolean();
                for (int i = random.nextInt(9) + 1; i > 0; i--) {
                    val = b ? Math.nextUp(val) : Math.nextDown(val);
                }
            }

            if (random.nextFloat() < chanceSpecialOffset) {
                val += (random.nextBoolean() ? 1 : -1) * specialValue;
            }

            if (random.nextFloat() < chanceBigFuzz) {
                val += random.nextGaussian();
            }
        }
        return val;
    }

    private static class VoxelShapeVoxelShapePair {
        final VoxelShape a, b;
        final BooleanBiFunction function;

        private VoxelShapeVoxelShapePair(VoxelShape a, VoxelShape b, BooleanBiFunction function) {
            this.a = a;
            this.b = b;
            this.function = function;
        }
    }
}
