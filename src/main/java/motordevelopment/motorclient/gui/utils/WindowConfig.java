/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.utils;

import motordevelopment.motorclient.utils.misc.ISerializable;
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
        expanded = tag.getBoolean("expanded");
        x = tag.getDouble("x");
        y = tag.getDouble("y");

        return this;
    }
}
