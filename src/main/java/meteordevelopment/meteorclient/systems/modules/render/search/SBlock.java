/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.search;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class SBlock {
    private static final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private static final Search search = Modules.get().get(Search.class);

    public static final int FO = 1 << 1;
    public static final int FO_RI = 1 << 2;
    public static final int RI = 1 << 3;
    public static final int BA_RI = 1 << 4;
    public static final int BA = 1 << 5;
    public static final int BA_LE = 1 << 6;
    public static final int LE = 1 << 7;
    public static final int FO_LE = 1 << 8;

    public static final int TO = 1 << 9;
    public static final int TO_FO = 1 << 10;
    public static final int TO_BA = 1 << 11;
    public static final int TO_RI = 1 << 12;
    public static final int TO_LE = 1 << 13;
    public static final int BO = 1 << 14;
    public static final int BO_FO = 1 << 15;
    public static final int BO_BA = 1 << 16;
    public static final int BO_RI = 1 << 17;
    public static final int BO_LE = 1 << 18;

    public static final int[] SIDES = { FO, BA, LE, RI, TO, BO };

    public final int x, y, z;
    private BlockState state;
    public int neighbours;

    public SGroup group;

    public boolean loaded = true;

    public SBlock(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public SBlock getSideBlock(int side) {
        switch (side) {
            case FO: return search.getBlock(x, y, z + 1);
            case BA: return search.getBlock(x, y, z - 1);
            case LE: return search.getBlock(x - 1, y, z);
            case RI: return search.getBlock(x + 1, y, z);
            case TO: return search.getBlock(x, y + 1, z);
            case BO: return search.getBlock(x, y - 1, z);
        }

        return null;
    }

    private void assignGroup() {
        SGroup firstGroup = null;

        for (int side : SIDES) {
            if ((neighbours & side) != side) continue;

            SBlock neighbour = getSideBlock(side);
            if (neighbour == null || neighbour.group == null) continue;

            if (firstGroup == null) {
                firstGroup = neighbour.group;
            }
            else {
                if (firstGroup != neighbour.group) firstGroup.merge(neighbour.group);
            }
        }

        if (firstGroup == null) {
            firstGroup = search.newGroup(state.getBlock());
        }

        firstGroup.add(this);
    }

    public void update() {
        state = mc.world.getBlockState(blockPos.set(x, y, z));
        neighbours = 0;

        if (isNeighbour(Direction.SOUTH)) neighbours |= FO;
        if (isNeighbourDiagonal(1, 0, 1)) neighbours |= FO_RI;
        if (isNeighbour(Direction.EAST)) neighbours |= RI;
        if (isNeighbourDiagonal(1, 0, -1)) neighbours |= BA_RI;
        if (isNeighbour(Direction.NORTH)) neighbours |= BA;
        if (isNeighbourDiagonal(-1, 0, -1)) neighbours |= BA_LE;
        if (isNeighbour(Direction.WEST)) neighbours |= LE;
        if (isNeighbourDiagonal(-1, 0, 1)) neighbours |= FO_LE;

        if (isNeighbour(Direction.UP)) neighbours |= TO;
        if (isNeighbourDiagonal(0, 1, 1)) neighbours |= TO_FO;
        if (isNeighbourDiagonal(0, 1, -1)) neighbours |= TO_BA;
        if (isNeighbourDiagonal(1, 1, 0)) neighbours |= TO_RI;
        if (isNeighbourDiagonal(-1, 1, 0)) neighbours |= TO_LE;
        if (isNeighbour(Direction.DOWN)) neighbours |= BO;
        if (isNeighbourDiagonal(0, -1, 1)) neighbours |= BO_FO;
        if (isNeighbourDiagonal(0, -1, -1)) neighbours |= BO_BA;
        if (isNeighbourDiagonal(1, -1, 0)) neighbours |= BO_RI;
        if (isNeighbourDiagonal(-1, -1, 0)) neighbours |= BO_LE;

        if (group == null) assignGroup();
    }

    private boolean isNeighbour(Direction dir) {
        blockPos.set(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ());
        BlockState neighbourState = mc.world.getBlockState(blockPos);

        if (neighbourState.getBlock() != state.getBlock()) return false;

        VoxelShape cube = VoxelShapes.fullCube();
        VoxelShape shape = state.getOutlineShape(mc.world, blockPos);
        VoxelShape neighbourShape = neighbourState.getOutlineShape(mc.world, blockPos);

        if (shape.isEmpty()) shape = cube;
        if (neighbourShape.isEmpty()) neighbourShape = cube;

        switch (dir) {
            case SOUTH:
                if (shape.getMax(Direction.Axis.Z) == 1 && neighbourShape.getMin(Direction.Axis.Z) == 0) return true;
                break;

            case NORTH:
                if (shape.getMin(Direction.Axis.Z) == 0 && neighbourShape.getMax(Direction.Axis.Z) == 1) return true;
                break;

            case EAST:
                if (shape.getMax(Direction.Axis.X) == 1 && neighbourShape.getMin(Direction.Axis.X) == 0) return true;
                break;

            case WEST:
                if (shape.getMin(Direction.Axis.X) == 0 && neighbourShape.getMax(Direction.Axis.X) == 1) return true;
                break;

            case UP:
                if (shape.getMax(Direction.Axis.Y) == 1 && neighbourShape.getMin(Direction.Axis.Y) == 0) return true;
                break;

            case DOWN:
                if (shape.getMin(Direction.Axis.Y) == 0 && neighbourShape.getMax(Direction.Axis.Y) == 1) return true;
                break;
        }

        return false;
    }

    private boolean isNeighbourDiagonal(double x, double y, double z) {
        blockPos.set(this.x + x, this.y + y, this.z + z);
        return state.getBlock() == mc.world.getBlockState(blockPos).getBlock();
    }

    public void render(Render3DEvent event) {
        double x1 = x;
        double y1 = y;
        double z1 = z;
        double x2 = x + 1;
        double y2 = y + 1;
        double z2 = z + 1;

        VoxelShape shape = state.getOutlineShape(mc.world, blockPos);

        if (!shape.isEmpty()) {
            x1 = x + shape.getMin(Direction.Axis.X);
            y1 = y + shape.getMin(Direction.Axis.Y);
            z1 = z + shape.getMin(Direction.Axis.Z);
            x2 = x + shape.getMax(Direction.Axis.X);
            y2 = y + shape.getMax(Direction.Axis.Y);
            z2 = z + shape.getMax(Direction.Axis.Z);
        }

        SBlockData blockData = search.getBlockData(state.getBlock());

        ShapeMode shapeMode = blockData.shapeMode;
        Color lineColor = blockData.lineColor;
        Color sideColor = blockData.sideColor;

        if (neighbours == 0) {
            event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, shapeMode, 0);
        }
        else {
            // Lines
            if (shapeMode.lines()) {
                // Vertical, BA_LE
                if (((neighbours & LE) != LE && (neighbours & BA) != BA) || ((neighbours & LE) == LE && (neighbours & BA) == BA && (neighbours & BA_LE) != BA_LE)) {
                    event.renderer.line(x1, y1, z1, x1, y2, z1, lineColor);
                }
                // Vertical, FO_LE
                if (((neighbours & LE) != LE && (neighbours & FO) != FO) || ((neighbours & LE) == LE && (neighbours & FO) == FO && (neighbours & FO_LE) != FO_LE)) {
                    event.renderer.line(x1, y1, z2, x1, y2, z2, lineColor);
                }
                // Vertical, BA_RI
                if (((neighbours & RI) != RI && (neighbours & BA) != BA) || ((neighbours & RI) == RI && (neighbours & BA) == BA && (neighbours & BA_RI) != BA_RI)) {
                    event.renderer.line(x2, y1, z1, x2, y2, z1, lineColor);
                }
                // Vertical, FO_RI
                if (((neighbours & RI) != RI && (neighbours & FO) != FO) || ((neighbours & RI) == RI && (neighbours & FO) == FO && (neighbours & FO_RI) != FO_RI)) {
                    event.renderer.line(x2, y1, z2, x2, y2, z2, lineColor);
                }

                // Horizontal bottom, BA_LE - BA_RI
                if (((neighbours & BA) != BA && (neighbours & BO) != BO) || ((neighbours & BA) != BA && (neighbours & BO_BA) == BO_BA)) {
                    event.renderer.line(x1, y1, z1, x2, y1, z1, lineColor);
                }
                // Horizontal bottom, FO_LE - FO_RI
                if (((neighbours & FO) != FO && (neighbours & BO) != BO) || ((neighbours & FO) != FO && (neighbours & BO_FO) == BO_FO)) {
                    event.renderer.line(x1, y1, z2, x2, y1, z2, lineColor);
                }
                // Horizontal top, BA_LE - BA_RI
                if (((neighbours & BA) != BA && (neighbours & TO) != TO) || ((neighbours & BA) != BA && (neighbours & TO_BA) == TO_BA)) {
                    event.renderer.line(x1, y2, z1, x2, y2, z1, lineColor);
                }
                // Horizontal top, FO_LE - FO_RI
                if (((neighbours & FO) != FO && (neighbours & TO) != TO) || ((neighbours & FO) != FO && (neighbours & TO_FO) == TO_FO)) {
                    event.renderer.line(x1, y2, z2, x2, y2, z2, lineColor);
                }

                // Horizontal bottom, BA_LE - FO_LE
                if (((neighbours & LE) != LE && (neighbours & BO) != BO) || ((neighbours & LE) != LE && (neighbours & BO_LE) == BO_LE)) {
                    event.renderer.line(x1, y1, z1, x1, y1, z2, lineColor);
                }
                // Horizontal bottom, BA_RI - FO_RI
                if (((neighbours & RI) != RI && (neighbours & BO) != BO) || ((neighbours & RI) != RI && (neighbours & BO_RI) == BO_RI)) {
                    event.renderer.line(x2, y1, z1, x2, y1, z2, lineColor);
                }
                // Horizontal top, BA_LE - FO_LE
                if (((neighbours & LE) != LE && (neighbours & TO) != TO) || ((neighbours & LE) != LE && (neighbours & TO_LE) == TO_LE)) {
                    event.renderer.line(x1, y2, z1, x1, y2, z2, lineColor);
                }
                // Horizontal top, BA_RI - FO_RI
                if (((neighbours & RI) != RI && (neighbours & TO) != TO) || ((neighbours & RI) != RI && (neighbours & TO_RI) == TO_RI)) {
                    event.renderer.line(x2, y2, z1, x2, y2, z2, lineColor);
                }
            }

            // Sides
            if (shapeMode.sides()) {
                // Bottom
                if ((neighbours & BO) != BO) {
                    event.renderer.quadHorizontal(x1, y1, z1, x2, z2, sideColor);
                }
                // Top
                if ((neighbours & TO) != TO) {
                    event.renderer.quadHorizontal(x1, y2, z1, x2, z2, sideColor);
                }
                // Front
                if ((neighbours & FO) != FO) {
                    event.renderer.quadVertical(x1, y1, z2, x2, y2, z2, sideColor);
                }
                // Back
                if ((neighbours & BA) != BA) {
                    event.renderer.quadVertical(x1, y1, z1, x2, y2, z1, sideColor);
                }
                // Right
                if ((neighbours & RI) != RI) {
                    event.renderer.quadVertical(x2, y1, z1, x2, y2, z2, sideColor);
                }
                // Left
                if ((neighbours & LE) != LE) {
                    event.renderer.quadVertical(x1, y1, z1, x1, y2, z2, sideColor);
                }
            }
        }
    }


    public static long getKey(int x, int y, int z) {
        return ((long) y << 16) | ((long) (z & 15) << 8) | ((long) (x & 15));
    }

    public static long getKey(BlockPos blockPos) {
        return getKey(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }
}
