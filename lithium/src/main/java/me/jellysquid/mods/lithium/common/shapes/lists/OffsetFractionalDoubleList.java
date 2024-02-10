package me.jellysquid.mods.lithium.common.shapes.lists;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;

public class OffsetFractionalDoubleList extends AbstractDoubleList {
    //this class must not extend FractionalDoubleList, due to VoxelShapes.createListPair using instanceof
    private final int numSections;
    private final double offset;

    public OffsetFractionalDoubleList(int numSections, double offset) {
        this.numSections = numSections;
        this.offset = offset;
    }

    public double getDouble(int position) {
        return this.offset + (double) position / (double) this.numSections;
    }

    public int size() {
        return this.numSections + 1;
    }

}
