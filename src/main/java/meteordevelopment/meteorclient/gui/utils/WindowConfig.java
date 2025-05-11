/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.utils;

import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.NbtCompound;

public class WindowConfig implements ISerializable<WindowConfig> {
    public boolean expanded = true;
    public double x = -1;
    public double y = -1;

    // Saving

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putBoolean("expanded", expanded);
        tag.putDouble("x", x);
        tag.putDouble("y", y);

        return tag;
    }

    @Override
    public WindowConfig fromTag(NbtCompound tag) {
        tag.getBoolean("expanded").ifPresent(bool -> expanded = bool);
        tag.getDouble("x").ifPresent(x1 -> x = x1);
        tag.getDouble("y").ifPresent(y1 -> y = y1);

        return this;
    }
}
