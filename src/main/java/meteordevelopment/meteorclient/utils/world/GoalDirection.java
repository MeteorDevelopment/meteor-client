/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.SettingsUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class GoalDirection implements Goal {
    private static final double SQRT_2 = Math.sqrt(2.0D);
    private final float yaw;
    private int x;
    private int z;

    public GoalDirection(Vec3d origin, float yaw) {
        this.yaw = yaw;
        recalculate(origin);
    }

    public static double calculate(double xDiff, double zDiff) {
        double x = Math.abs(xDiff);
        double z = Math.abs(zDiff);
        double straight;
        double diagonal;
        if (x < z) {
            straight = z - x;
            diagonal = x;
        } else {
            straight = x - z;
            diagonal = z;
        }

        diagonal *= SQRT_2;
        return (diagonal + straight) * BaritoneAPI.getSettings().costHeuristic.value;
    }

    public void recalculate(Vec3d origin) {
        float theta = (float) Math.toRadians(yaw);
        x = (int) Math.floor(origin.x - (double) MathHelper.sin(theta) * 100);
        z = (int) Math.floor(origin.z + (double) MathHelper.cos(theta) * 100);
    }

    public boolean isInGoal(int x, int y, int z) {
        return x == this.x && z == this.z;
    }

    public double heuristic(int x, int y, int z) {
        int xDiff = x - this.x;
        int zDiff = z - this.z;
        return calculate(xDiff, zDiff);
    }

    public String toString() {
        return String.format("GoalXZ{x=%s,z=%s}", SettingsUtil.maybeCensor(this.x), SettingsUtil.maybeCensor(this.z));
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }
}
