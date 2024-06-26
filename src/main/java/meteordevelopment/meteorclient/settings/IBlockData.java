/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import meteordevelopment.meteorclient.utils.misc.ICopyable;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.block.Block;

public interface IBlockData<T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> {
    WidgetScreen createScreen(GuiTheme theme, Block block, BlockDataSetting<T> setting);
}
