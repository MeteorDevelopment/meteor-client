/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.utils;

import minegame159.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;

public class WindowConfig implements ISerializable<WindowConfig> {
    public boolean expanded = true;
    public double x = -1;
    public double y = -1;

    // Saving

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("expanded", expanded);
        tag.putDouble("x", x);
        tag.putDouble("y", y);

        return tag;
    }

    @Override
    public WindowConfig fromTag(CompoundTag tag) {
        expanded = tag.getBoolean("expanded");
        x = tag.getDouble("x");
        y = tag.getDouble("y");

        return this;
    }
}
