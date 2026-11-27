/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Estimates stronghold locations from an Eye of Ender bearing or a sampled flight path. */
public final class StrongholdLocator {
    public static final double DEFAULT_ANGLE_STANDARD_DEVIATION = 0.1;

    private static final int SNAPPING_RADIUS = 7;
    private static final double CHUNK_SIZE = 16.0;
    private static final int DISTANCE_PARAMETER = 32;
    private static final int TOTAL_STRONGHOLDS = 128;
    private static final int RING_COUNT = 8;
    private static final int CLOSEST_INTEGRAL_STEPS = 7;
    private static final int MAX_CHUNK = (int) (DISTANCE_PARAMETER * ((4.0 + (RING_COUNT - 1) * 6.0) + 1.25) + 2 * SNAPPING_RADIUS + 1);
    private static final double MAX_RAY_LENGTH = 5000.0 / CHUNK_SIZE;
    private static final double SQRT_TWO = Math.sqrt(2.0);
    private static final double TWO_PI = 2.0 * Math.PI;

    private static final double STRONGHOLD_CHUNK_OFFSET = 0.0;

    private static final Ring[] RINGS = createRings();
    private static final double MAX_RADIUS = RINGS[RINGS.length - 1].outerRadiusAfterSnapping;
    private static final Density DENSITY = new Density();

    private StrongholdLocator() {
    }

    // Score chunk candidates against the measured bearing and ring layout.
    public static List<Prediction> predict(double playerX, double playerZ, double measuredYaw, double sigmaDegrees, int maximumPredictions) {
        if (!Double.isFinite(playerX) || !Double.isFinite(playerZ) || !Double.isFinite(measuredYaw)) {
            throw new IllegalArgumentException("Player position and measured yaw must be finite.");
        }
        if (!Double.isFinite(sigmaDegrees) || sigmaDegrees <= 0.0 || sigmaDegrees > 0.1) {
            throw new IllegalArgumentException("Angle standard deviation must be greater than 0 and at most 0.1 degrees.");
        }
        if (maximumPredictions <= 0) return List.of();

        List<WeightedChunk> candidates = createCandidates(playerX, playerZ, measuredYaw, sigmaDegrees);
        if (candidates.isEmpty()) return List.of();

        double maxLogWeight = candidates.stream().mapToDouble(candidate -> candidate.logWeight).max().orElse(Double.NEGATIVE_INFINITY);
        for (WeightedChunk candidate : candidates) {
            candidate.weight = Math.exp(candidate.logWeight - maxLogWeight);
        }
        if (!normalise(candidates)) return List.of();

        double[] angleOnlyWeights = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            angleOnlyWeights[i] = candidates.get(i).weight;
        }

        applyClosestStrongholdCondition(candidates, playerX / CHUNK_SIZE, playerZ / CHUNK_SIZE);
        if (!normalise(candidates)) {
            for (int i = 0; i < candidates.size(); i++) {
                candidates.get(i).weight = angleOnlyWeights[i];
            }
            normalise(candidates);
        }

        candidates.sort(Comparator.comparingDouble((WeightedChunk candidate) -> candidate.weight).reversed());

        int resultCount = Math.min(maximumPredictions, candidates.size());
        List<Prediction> predictions = new ArrayList<>(resultCount);
        for (int i = 0; i < resultCount; i++) {
            WeightedChunk candidate = candidates.get(i);
            predictions.add(new Prediction(candidate.chunkX, candidate.chunkZ, candidate.weight));
        }
        return List.copyOf(predictions);
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

    // Search the narrow wedge around the measured bearing.
    private static List<WeightedChunk> createCandidates(double playerX, double playerZ, double measuredYaw, double sigmaDegrees) {
        double playerChunkX = (playerX - STRONGHOLD_CHUNK_OFFSET) / CHUNK_SIZE;
        double playerChunkZ = (playerZ - STRONGHOLD_CHUNK_OFFSET) / CHUNK_SIZE;

        double angle = Math.toRadians(measuredYaw);
        double directionX = -Math.sin(angle);
        double directionZ = Math.cos(angle);

        double tolerance = Math.toRadians(Math.min(1.0, 30.0 * sigmaDegrees));
        double lowerAngle = angle - tolerance;
        double upperAngle = angle + tolerance;

        double lowerX = -Math.sin(lowerAngle);
        double lowerZ = Math.cos(lowerAngle);
        double upperX = -Math.sin(upperAngle);
        double upperZ = Math.cos(upperAngle);

        boolean majorX = Math.abs(directionX) >= Math.abs(directionZ);
        boolean majorPositive = majorX ? directionX > 0.0 : directionZ > 0.0;

        double originMajor = majorX ? playerChunkX : playerChunkZ;
        double originMinor = majorX ? playerChunkZ : playerChunkX;

        double iterStartMajor = getIterStartMajor(
            originMajor,
            originMinor,
            lowerX,
            lowerZ,
            upperX,
            upperZ,
            majorX,
            majorPositive
        );

        double lowerSlope = majorX ? lowerZ / lowerX : lowerX / lowerZ;
        double upperSlope = majorX ? upperZ / upperX : upperX / upperZ;
        boolean rightPositive = majorPositive
            ? upperSlope - lowerSlope > 0.0
            : lowerSlope - upperSlope > 0.0;

        double directionMajor = majorX ? directionX : directionZ;
        int major = (int) (majorPositive ? Math.ceil(iterStartMajor) : Math.floor(iterStartMajor));

        List<WeightedChunk> candidates = new ArrayList<>();
        while ((major - iterStartMajor) / directionMajor < MAX_RAY_LENGTH) {
            double minorLower = originMinor + lowerSlope * (major - originMajor);
            double minorUpper = originMinor + upperSlope * (major - originMajor);

            int minor = (int) (rightPositive ? Math.ceil(minorLower) : Math.floor(minorLower));
            minor = Math.max(-MAX_CHUNK, Math.min(MAX_CHUNK, minor));

            while (rightPositive ? minor < minorUpper && minor <= MAX_CHUNK
                : minor > minorUpper && minor >= -MAX_CHUNK) {
                int chunkX = majorX ? major : minor;
                int chunkZ = majorX ? minor : major;

                Ring ring = getRing(Math.hypot(chunkX, chunkZ));
                if (ring != null) {

                    // Average the density across the four chunk corners.
                    double prior = (
                        DENSITY.density(chunkX - 0.5, chunkZ - 0.5)
                            + DENSITY.density(chunkX - 0.5, chunkZ + 0.5)
                            + DENSITY.density(chunkX + 0.5, chunkZ - 0.5)
                            + DENSITY.density(chunkX + 0.5, chunkZ + 0.5)
                    ) * 0.25;

                    if (prior > 0.0) {
                        double deltaX = chunkX - playerChunkX;
                        double deltaZ = chunkZ - playerChunkZ;

                        if (deltaX * directionX + deltaZ * directionZ > 0.0) {
                            double expectedYaw = Math.toDegrees(-Math.atan2(deltaX, deltaZ));
                            double angleError = wrapDegrees(measuredYaw - expectedYaw);

                            if (Math.abs(angleError) <= Math.toDegrees(tolerance)) {
                                // Apply the angular likelihood to the ring prior.
                                double logWeight = Math.log(prior)
                                    - angleError * angleError / (2.0 * sigmaDegrees * sigmaDegrees);
                                candidates.add(new WeightedChunk(chunkX, chunkZ, logWeight));
                            }
                        }
                    }
                }

                minor += rightPositive ? 1 : -1;
            }

            major += majorPositive ? 1 : -1;
        }

        return candidates;
    }

    private static double getIterStartMajor(
        double originMajor,
        double originMinor,
        double lowerX,
        double lowerZ,
        double upperX,
        double upperZ,
        boolean majorX,
        boolean majorPositive
    ) {
        if (originMajor * originMajor + originMinor * originMinor <= MAX_CHUNK * MAX_CHUNK) {
            return originMajor;
        }

        double originX = majorX ? originMajor : originMinor;
        double originZ = majorX ? originMinor : originMajor;

        double lowerOrthogonal = orthogonalComponent(-originX, -originZ, lowerX, lowerZ);
        double upperOrthogonal = orthogonalComponent(-originX, -originZ, upperX, upperZ);
        if (lowerOrthogonal > 0.0 && upperOrthogonal < 0.0) {
            double originLength = Math.hypot(originX, originZ);
            double intersectionX = originX / originLength * MAX_CHUNK;
            double intersectionZ = originZ / originLength * MAX_CHUNK;

            double first = originMajor + projectAndGetMajorComponent(
                intersectionX - originX, intersectionZ - originZ, lowerX, lowerZ, majorX);
            double second = originMajor + projectAndGetMajorComponent(
                intersectionX - originX, intersectionZ - originZ, upperX, upperZ, majorX);

            return majorPositive ^ (first > second) ? first : second;
        }

        double lowerIntersection = findCircleIntersection(originX, originZ, lowerX, lowerZ, MAX_CHUNK, majorX);
        double upperIntersection = findCircleIntersection(originX, originZ, upperX, upperZ, MAX_CHUNK, majorX);

        if (lowerIntersection != 0.0 || upperIntersection != 0.0) {
            if (lowerIntersection != 0.0 && upperIntersection != 0.0) {
                return majorPositive ^ (lowerIntersection > upperIntersection) ? lowerIntersection : upperIntersection;
            }
            return lowerIntersection != 0.0 ? lowerIntersection : upperIntersection;
        }

        return originMajor;
    }

    private static double orthogonalComponent(double x, double z, double unitX, double unitZ) {
        double parallelMagnitude = unitX * x + unitZ * z;
        double orthogonalX = unitX * parallelMagnitude - x;
        double orthogonalZ = unitZ * parallelMagnitude - z;
        return unitZ * orthogonalX - unitX * orthogonalZ;
    }

    private static double projectAndGetMajorComponent(
        double x, double z, double unitX, double unitZ, boolean majorX
    ) {
        double projection = x * unitX + z * unitZ;
        return majorX ? unitX * projection : unitZ * projection;
    }

    private static double findCircleIntersection(
        double originX, double originZ, double unitX, double unitZ, double radius, boolean majorX
    ) {
        double originDotDirection = originX * unitX + originZ * unitZ;
        double discriminant = originDotDirection * originDotDirection
            + radius * radius - originX * originX - originZ * originZ;
        if (discriminant < 0.0) return 0.0;

        double distance = -originDotDirection - Math.sqrt(discriminant);
        return majorX ? originX + distance * unitX : originZ + distance * unitZ;
    }

    // Account for the fact that the nearest generated stronghold is the one found first.
    private static void applyClosestStrongholdCondition(List<WeightedChunk> candidates, double playerChunkX, double playerChunkZ) {
        candidates.sort(Comparator.comparingDouble((WeightedChunk candidate) -> candidate.weight).reversed());

        double playerRadius = Math.hypot(playerChunkX, playerChunkZ);
        double maximumNearestDistance = getMaximumNearestDistance(playerRadius);
        double accumulatedProbability = 0.0;
        int sampledCandidates = 0;

        for (int i = 0; i < candidates.size(); i++) {
            WeightedChunk candidate = candidates.get(i);

            if (i < 100 || candidate.weight > 0.001) {
                double probability = closestStrongholdProbability(candidate, playerChunkX, playerChunkZ, maximumNearestDistance);
                candidate.weight *= probability;
                accumulatedProbability += probability;
                sampledCandidates++;
            } else {
                candidate.weight *= sampledCandidates == 0 ? 1.0 : accumulatedProbability / sampledCandidates;
            }
        }
    }

    private static double closestStrongholdProbability(WeightedChunk candidate, double playerChunkX, double playerChunkZ, double maximumNearestDistance) {
        Ring candidateRing = getRing(Math.hypot(candidate.chunkX, candidate.chunkZ));
        if (candidateRing == null) return 0.0;

        double deltaX = candidate.chunkX - playerChunkX;
        double deltaZ = candidate.chunkZ - playerChunkZ;
        double candidateDistance = Math.hypot(deltaX, deltaZ);
        if (candidateDistance < 1e-9) return 0.0;

        double playerRadius = Math.hypot(playerChunkX, playerChunkZ);
        double candidatePhi = polarAngle(candidate.chunkX, candidate.chunkZ);
        double playerPhi = polarAngle(playerChunkX, playerChunkZ);
        double minimumRelevantRadius = playerRadius - maximumNearestDistance;
        double maximumRelevantRadius = playerRadius + maximumNearestDistance;

        double probability = 1.0;
        for (Ring ring : RINGS) {
            if (maximumRelevantRadius < ring.innerRadius || minimumRelevantRadius > ring.outerRadius) continue;

            boolean sameRing = candidateRing.index == ring.index;
            double angularStep = sameRing
                ? (2.0 / (2.0 * CLOSEST_INTEGRAL_STEPS + 1.0)) * (15.0 * SQRT_TWO / candidateRing.innerRadius)
                : (2.0 / (2.0 * CLOSEST_INTEGRAL_STEPS + 1.0)) * Math.PI / ring.strongholdCount;

            for (int stronghold = 0; stronghold < ring.strongholdCount; stronghold++) {
                if (sameRing && stronghold == 0) continue;

                double competitorCloserProbability = integrateCompetitorCloserProbability(
                    ring,
                    stronghold,
                    candidatePhi,
                    angularStep,
                    playerPhi,
                    playerRadius,
                    candidateDistance,
                    sameRing
                );
                probability *= 1.0 - competitorCloserProbability;
                if (probability <= 0.0) return 0.0;
            }
        }

        return clamp(probability, 0.0, 1.0);
    }

    // Integrate the probability that another stronghold is closer.
    private static double integrateCompetitorCloserProbability(
        Ring ring,
        int strongholdIndex,
        double candidatePhi,
        double angularStep,
        double playerPhi,
        double playerRadius,
        double candidateDistance,
        boolean sameRing
    ) {
        double meanPhi = candidatePhi + strongholdIndex * TWO_PI / ring.strongholdCount;
        double densityIntegral = 0.0;
        double probabilityIntegral = 0.0;

        for (int step = -CLOSEST_INTEGRAL_STEPS; step <= CLOSEST_INTEGRAL_STEPS; step++) {
            double deltaPhi = step * angularStep;
            double angularDensity = 1.0;
            if (sameRing) {
                double scaled = deltaPhi * ring.innerRadius / (15.0 * SQRT_TWO);
                angularDensity = Math.pow(1.0 + scaled, 4.5) * Math.pow(1.0 - scaled, 4.5);
            }
            densityIntegral += angularDensity * angularStep;

            double competitorPhi = meanPhi + deltaPhi;
            double gamma = playerPhi - competitorPhi;
            double sinGamma = Math.sin(gamma);
            if (Math.abs(sinGamma) < 1e-12) continue;

            double sinBeta = playerRadius / candidateDistance * sinGamma;
            if (sinBeta <= -1.0 || sinBeta >= 1.0) continue;

            double beta = Math.asin(sinBeta);
            double alpha0 = beta - gamma;
            double alpha1 = Math.PI - gamma - beta;
            double radius0 = candidateDistance * Math.sin(alpha0) / sinGamma;
            double radius1 = candidateDistance * Math.sin(alpha1) / sinGamma;

            radius0 = clamp(radius0, ring.innerRadiusAfterSnapping, ring.outerRadiusAfterSnapping);
            radius1 = clamp(radius1, ring.innerRadiusAfterSnapping, ring.outerRadiusAfterSnapping);

            double radialMass = DENSITY.cumulativePolar(radius1) - DENSITY.cumulativePolar(radius0);
            probabilityIntegral += angularDensity * Math.max(0.0, radialMass) * angularStep / ring.strongholdCount;
        }

        if (densityIntegral <= 0.0) return 0.0;
        return clamp(probabilityIntegral / densityIntegral, 0.0, 1.0);
    }

    private static double getMaximumNearestDistance(double playerRadius) {
        double maximumDistance = Double.POSITIVE_INFINITY;

        for (Ring ring : RINGS) {
            double innerDistanceSquared = ring.innerRadius * ring.innerRadius
                + playerRadius * playerRadius
                - 2.0 * playerRadius * ring.innerRadius * Math.cos(Math.PI / ring.strongholdCount);
            double outerDistanceSquared = ring.outerRadius * ring.outerRadius
                + playerRadius * playerRadius
                - 2.0 * playerRadius * ring.outerRadius * Math.cos(Math.PI / ring.strongholdCount);
            double ringMaximum = Math.sqrt(Math.max(0.0, Math.max(innerDistanceSquared, outerDistanceSquared)));
            maximumDistance = Math.min(maximumDistance, ringMaximum);
        }

        return maximumDistance + SQRT_TWO * (SNAPPING_RADIUS + 0.5);
    }

    private static boolean normalise(List<WeightedChunk> candidates) {
        double total = 0.0;
        for (WeightedChunk candidate : candidates) {
            total += candidate.weight;
        }
        if (!Double.isFinite(total) || total <= 0.0) return false;

        for (WeightedChunk candidate : candidates) {
            candidate.weight /= total;
        }
        return true;
    }

    // Build the ring bands, capped at the world-generation total.
    private static Ring[] createRings() {
        Ring[] rings = new Ring[RING_COUNT];
        int strongholdsInPreviousRings = 0;
        int strongholdsInRing = 1;

        for (int i = 0; i < RING_COUNT; i++) {
            strongholdsInRing += 2 * strongholdsInRing / (i + 1);
            strongholdsInRing = Math.min(strongholdsInRing, TOTAL_STRONGHOLDS - strongholdsInPreviousRings);
            rings[i] = new Ring(i, strongholdsInRing);
            strongholdsInPreviousRings += strongholdsInRing;
        }

        return rings;
    }

    private static Ring getRing(double radius) {
        for (Ring ring : RINGS) {
            if (ring.containsAfterSnapping(radius)) return ring;
        }
        return null;
    }

    private static double polarAngle(double x, double z) {
        return -Math.atan2(x, z);
    }

    private static double wrapDegrees(double angle) {
        angle %= 360.0;
        if (angle < -180.0) angle += 360.0;
        else if (angle > 180.0) angle -= 360.0;
        return angle;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record TrajectoryPrediction(int blockX, int blockZ, long accuracy) {
    }

    public record Prediction(int chunkX, int chunkZ, double probability) {
        public int blockX() {
            return (int) (chunkX * CHUNK_SIZE);
        }

        public int blockZ() {
            return (int) (chunkZ * CHUNK_SIZE);
        }
    }

    private static final class WeightedChunk {
        private final int chunkX;
        private final int chunkZ;
        private final double logWeight;
        private double weight;

        private WeightedChunk(int chunkX, int chunkZ, double logWeight) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.logWeight = logWeight;
        }
    }

    // Keep the generated ring bounds separate from their chunk-snapped bounds.
    private static final class Ring {
        private final int index;
        private final int strongholdCount;
        private final double innerRadius;
        private final double outerRadius;
        private final double innerRadiusAfterSnapping;
        private final double outerRadiusAfterSnapping;

        private Ring(int index, int strongholdCount) {
            this.index = index;
            this.strongholdCount = strongholdCount;

            innerRadius = DISTANCE_PARAMETER * ((4.0 + index * 6.0) - 1.25);
            outerRadius = DISTANCE_PARAMETER * ((4.0 + index * 6.0) + 1.25);

            double snappingMargin = (SNAPPING_RADIUS + 1.0) * SQRT_TWO;
            innerRadiusAfterSnapping = innerRadius - snappingMargin;
            outerRadiusAfterSnapping = outerRadius + snappingMargin;
        }

        private boolean containsAfterSnapping(double radius) {
            return radius >= innerRadiusAfterSnapping && radius <= outerRadiusAfterSnapping;
        }
    }

    private static final class Density {
        private static final int ANGLE_SAMPLES = 200;

        private final double[] density;
        private final double[] cumulativePolar;

        private Density() {
            density = new double[(int) Math.ceil(MAX_RADIUS) + 5];
            cumulativePolar = new double[density.length];

            for (Ring ring : RINGS) {
                int firstRadius = (int) ring.innerRadius;
                int lastRadius = (int) ring.outerRadius;

                for (int radius = firstRadius; radius <= lastRadius; radius++) {
                    if (radius == 0) continue;

                    double value = ring.strongholdCount
                        / (TWO_PI * (ring.outerRadius - ring.innerRadius) * radius);
                    if (radius == firstRadius || radius == lastRadius) value *= 0.5;
                    density[radius] += value;
                }
            }

            smoothForSnapping();
            buildCumulativePolarDensity();
        }

        // Smooth the radial density to account for chunk snapping.
        private void smoothForSnapping() {
            int[] offsetWeights = createOffsetWeights();
            int filterLength = (int) Math.ceil(SNAPPING_RADIUS * SQRT_TWO) + 1;
            double[] filter = new double[filterLength];
            double weightSum = 0.0;

            for (int x = -SNAPPING_RADIUS; x <= SNAPPING_RADIUS; x++) {
                for (int z = -SNAPPING_RADIUS; z <= SNAPPING_RADIUS; z++) {
                    int weight = offsetWeights[x + SNAPPING_RADIUS] * offsetWeights[z + SNAPPING_RADIUS];
                    double offsetRadius = Math.hypot(x, z);

                    for (int i = 0; i < ANGLE_SAMPLES; i++) {
                        double angle = TWO_PI * i / ANGLE_SAMPLES;
                        int radialOffset = Math.abs((int) Math.round(offsetRadius * Math.sin(angle)));
                        filter[radialOffset] += weight;
                        weightSum += radialOffset == 0 ? weight : 2.0 * weight;
                    }
                }
            }

            for (int i = 0; i < filter.length; i++) {
                filter[i] /= weightSum;
            }

            double[] originalDensity = density.clone();
            for (int radius = 0; radius < density.length; radius++) {
                double smoothed = 0.0;
                for (int offset = -filter.length + 1; offset < filter.length; offset++) {
                    int sourceRadius = radius + offset;
                    if (sourceRadius < 0 || sourceRadius >= originalDensity.length) continue;
                    smoothed += originalDensity[sourceRadius] * filter[Math.abs(offset)];
                }
                density[radius] = smoothed;
            }
        }

        private static int[] createOffsetWeights() {
            Map<Integer, Integer> weights = new HashMap<>();
            for (int i = -26; i <= 30; i++) {
                int chunkOffset = i >> 2;
                weights.merge(-chunkOffset, 1, Integer::sum);
            }

            int[] result = new int[2 * SNAPPING_RADIUS + 1];
            for (int offset = -SNAPPING_RADIUS; offset <= SNAPPING_RADIUS; offset++) {
                result[offset + SNAPPING_RADIUS] = weights.getOrDefault(offset, 0);
            }
            return result;
        }

        // Precompute radial mass for the integration step.
        private void buildCumulativePolarDensity() {
            double sum = 0.0;
            for (int radius = 0; radius < density.length; radius++) {
                sum += density[radius] * radius * TWO_PI;
                cumulativePolar[radius] = sum;
            }
        }

        private double density(double x, double z) {
            double radius = Math.hypot(x, z);
            int lower = (int) radius;
            int upper = lower + 1;
            if (upper >= density.length) return 0.0;

            double fraction = radius - lower;
            return (1.0 - fraction) * density[lower] + fraction * density[upper];
        }

        private double cumulativePolar(double radius) {
            if (radius < 0.0) return 0.0;
            if (radius >= cumulativePolar.length - 1) return cumulativePolar[cumulativePolar.length - 1];

            int lower = (int) radius;
            int upper = lower + 1;
            double fraction = radius - lower;
            return (1.0 - fraction) * cumulativePolar[lower] + fraction * cumulativePolar[upper];
        }
    }
}
