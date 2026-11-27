/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Estimates stronghold locations from a sampled Eye of Ender flight path. */
public final class StrongholdLocator {
    private static final double CHUNK_SIZE = 16.0;

    private StrongholdLocator() {
    }

    // Find chunk positions where the observed flight path aligns with the stronghold rings.
    public static List<TrajectoryPrediction> predictFromTrajectory(
        double startX,
        double startZ,
        double endX,
        double endZ,
        int maximumPredictions
    ) {
        if (!Double.isFinite(startX) || !Double.isFinite(startZ)
            || !Double.isFinite(endX) || !Double.isFinite(endZ)) {
            throw new IllegalArgumentException("Eye trajectory positions must be finite.");
        }
        if (maximumPredictions <= 0) return List.of();

        double deltaX = endX - startX;
        double deltaZ = endZ - startZ;
        if (deltaX * deltaX + deltaZ * deltaZ < 4.0) return List.of();

        boolean majorX = Math.abs(deltaX) >= Math.abs(deltaZ);
        double originMajor = majorX ? startX : startZ;
        double originMinor = majorX ? startZ : startX;
        double directionMajor = majorX ? deltaX : deltaZ;
        double directionMinor = majorX ? deltaZ : deltaX;
        double slope = directionMinor / directionMajor;
        double step = directionMajor > 0.0 ? CHUNK_SIZE : -CHUNK_SIZE;

        double major = Math.round(originMajor / CHUNK_SIZE) * CHUNK_SIZE;

        boolean foundRing = false;
        for (int i = 0; i < 2048; i++) {
            double minor = originMinor + (major - originMajor) * slope;
            if (isInStrongholdRing(majorX ? major : minor, majorX ? minor : major)) {
                foundRing = true;
                break;
            }
            major += step;
        }
        if (!foundRing) return List.of();

        List<TrajectoryPrediction> predictions = new ArrayList<>();
        for (int i = 0; i < 2048; i++) {
            double minor = originMinor + (major - originMajor) * slope;
            double worldX = majorX ? major : minor;
            double worldZ = majorX ? minor : major;

            if (!isInStrongholdRing(worldX, worldZ)) break;

            double alignedMinor = Math.round(minor / CHUNK_SIZE) * CHUNK_SIZE;
            double alignmentError = Math.abs(alignedMinor - minor);
            long accuracy = alignmentError < 1.0e-9
                ? Long.MAX_VALUE
                : (long) (1.0 / alignmentError);

            if (accuracy > 2) {
                int blockX = (int) (Math.round((majorX ? major : alignedMinor) / CHUNK_SIZE) * CHUNK_SIZE);
                int blockZ = (int) (Math.round((majorX ? alignedMinor : major) / CHUNK_SIZE) * CHUNK_SIZE);
                predictions.add(new TrajectoryPrediction(blockX, blockZ, accuracy));
            }

            major += step;
        }

        predictions.sort(Comparator.comparingLong(TrajectoryPrediction::accuracy).reversed());
        if (predictions.size() > maximumPredictions) {
            return List.copyOf(predictions.subList(0, maximumPredictions));
        }
        return List.copyOf(predictions);
    }

    private static boolean isInStrongholdRing(double x, double z) {
        long distance = Math.round(Math.hypot(x, z));
        return distance < 21248
            ? (distance + 240) % 3072 > 1504
            : distance < 24336;
    }

    public record TrajectoryPrediction(int blockX, int blockZ, long accuracy) {
    }
}
