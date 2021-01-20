/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.world;

import net.minecraft.util.math.Direction;

public class Dir {
    public static final byte UP = 1 << 1;
    public static final byte DOWN = 1 << 2;
    public static final byte NORTH = 1 << 3;
    public static final byte SOUTH = 1 << 4;
    public static final byte WEST = 1 << 5;
    public static final byte EAST = 1 << 6;

    public static byte get(Direction dir) {
        switch (dir) {
            case UP:    return UP;
            case DOWN:  return DOWN;
            case NORTH: return NORTH;
            case SOUTH: return SOUTH;
            case WEST:  return WEST;
            case EAST:  return EAST;
            default:    return 0;
        }
    }

    public static boolean is(int dir, byte idk) {
        return (dir & idk) != idk;
    }

    public static boolean isNot(int dir, byte idk) {
        return (dir & idk) == idk;
    }
}
