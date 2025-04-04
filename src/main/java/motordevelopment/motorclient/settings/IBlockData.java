/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.settings;

import motordevelopment.motorclient.gui.GuiTheme;
import motordevelopment.motorclient.gui.WidgetScreen;
import motordevelopment.motorclient.utils.misc.IChangeable;
import motordevelopment.motorclient.utils.misc.ICopyable;
import motordevelopment.motorclient.utils.misc.ISerializable;
import net.minecraft.block.Block;

public interface IBlockData<T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> {
    WidgetScreen createScreen(GuiTheme theme, Block block, BlockDataSetting<T> setting);
}
