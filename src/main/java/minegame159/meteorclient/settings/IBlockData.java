/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.utils.misc.IChangeable;
import minegame159.meteorclient.utils.misc.ICopyable;
import minegame159.meteorclient.utils.misc.ISerializable;
import net.minecraft.block.Block;

public interface IBlockData<T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> {
    WidgetScreen createScreen(GuiTheme theme, Block block, BlockDataSetting<T> setting);
}
