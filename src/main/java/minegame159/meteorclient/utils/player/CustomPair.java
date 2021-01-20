/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.player;

import minegame159.meteorclient.modules.Module;

import java.util.List;

public class CustomPair{
    private final Class<? extends Module> left;
    private final List<Integer> right;
    public CustomPair(Class<? extends Module> left, List<Integer> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CustomPair) {
            CustomPair o = (CustomPair) obj;
            return o.getLeft().getName().equals(left.getName()) && o.getRight().containsAll(right);
        }
        return false;
    }

    public Class<? extends Module> getLeft() {
        return left;
    }

    public List<Integer> getRight() {
        return right;
    }
}
