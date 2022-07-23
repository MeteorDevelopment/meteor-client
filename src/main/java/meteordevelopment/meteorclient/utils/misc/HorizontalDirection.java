/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

public enum HorizontalDirection {
    South("South", "Z+", false, 0, 0, 1),
    SouthEast("South East", "Z+ X+", true, -45, 1, 1),
    West("West", "X-", false, 90, -1, 0),
    NorthWest("North West", "Z- X-", true, 135, -1, -1),
    North("North", "Z-", false, 180, 0, -1),
    NorthEast("North East", "Z- X+", true, -135, 1, -1),
    East("East", "X+", false, -90, 1, 0),
    SouthWest("South West", "Z+ X-", true, 45, -1, 1);

    public final String name;
    public final String axis;
    public final boolean diagonal;
    public final float yaw;
    public final int offsetX, offsetZ;

    HorizontalDirection(String name, String axis, boolean diagonal, float yaw, int offsetX, int offsetZ) {
        this.axis = axis;
        this.name = name;
        this.diagonal = diagonal;
        this.yaw = yaw;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
    }

    public HorizontalDirection opposite() {
        return switch (this) {
            case South -> North;
            case SouthEast -> NorthWest;
            case West -> East;
            case NorthWest -> SouthEast;
            case North -> South;
            case NorthEast -> SouthWest;
            case East -> West;
            case SouthWest -> NorthEast;
        };
    }

    public HorizontalDirection rotateLeft() {
        return switch (this) {
            case South -> SouthEast;
            case SouthEast -> East;
            case East -> NorthEast;
            case NorthEast -> North;
            case North -> NorthWest;
            case NorthWest -> West;
            case West -> SouthWest;
            case SouthWest -> South;
        };
    }

    public HorizontalDirection rotateLeftSkipOne() {
        return switch (this) {
            case South -> East;
            case East -> North;
            case North -> West;
            case West -> South;
            case SouthEast -> NorthEast;
            case NorthEast -> NorthWest;
            case NorthWest -> SouthWest;
            case SouthWest -> SouthEast;
        };
    }

    public HorizontalDirection rotateRight() {
        return switch (this) {
            case South -> SouthWest;
            case SouthWest -> West;
            case West -> NorthWest;
            case NorthWest -> North;
            case North -> NorthEast;
            case NorthEast -> East;
            case East -> SouthEast;
            case SouthEast -> South;
        };
    }

    public static HorizontalDirection get(float yaw) {
        yaw = yaw % 360;
        if (yaw < 0) yaw += 360;

        if (yaw >= 337.5 || yaw < 22.5) return South;
        else if (yaw >= 22.5 && yaw < 67.5) return SouthWest;
        else if (yaw >= 67.5 && yaw < 112.5) return West;
        else if (yaw >= 112.5 && yaw < 157.5) return NorthWest;
        else if (yaw >= 157.5 && yaw < 202.5) return North;
        else if (yaw >= 202.5 && yaw < 247.5) return NorthEast;
        else if (yaw >= 247.5 && yaw < 292.5) return East;
        else if (yaw >= 292.5 && yaw < 337.5) return SouthEast;

        return South;
    }
}
