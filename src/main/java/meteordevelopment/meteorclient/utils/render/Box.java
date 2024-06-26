/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import java.util.Objects;

public class Box {
    public double x, y;
    public double width, height;

    public Box(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Box() {
        this(0, 0, 0, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Box box = (Box) o;
        return Double.compare(box.x, x) == 0 &&
                Double.compare(box.y, y) == 0 &&
                Double.compare(box.width, width) == 0 &&
                Double.compare(box.height, height) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, width, height);
    }
}
