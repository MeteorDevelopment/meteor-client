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
import net.minecraft.entity.EntityType;

public interface IEntityTypeData<T extends ICopyable<T> & ISerializable<T> & IChangeable & IEntityTypeData<T>> {
    WidgetScreen createScreen(GuiTheme theme, EntityType entityType, EntityTypeDataSetting<T> setting);
}
