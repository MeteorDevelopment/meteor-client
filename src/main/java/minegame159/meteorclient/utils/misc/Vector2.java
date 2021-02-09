/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.misc;

import net.minecraft.nbt.CompoundTag;

import java.util.Objects;

public class Vector2 implements ISerializable<Vector2> {
    public static final Vector2 ZERO = new Vector2(0, 0);

    public double x, y;

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2() {
        this(0, 0);
    }

    public Vector2(Vector2 other) {
        this(other.x, other.y);
    }

    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putDouble("x", x);
        tag.putDouble("y", y);

        return tag;
    }

    @Override
    public Vector2 fromTag(CompoundTag tag) {
        x = tag.getDouble("x");
        y = tag.getDouble("y");

        return this;
    }

    @Override
    public String toString() {
        return x + ", " + y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector2 vector2 = (Vector2) o;
        return Double.compare(vector2.x, x) == 0 &&
                Double.compare(vector2.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
