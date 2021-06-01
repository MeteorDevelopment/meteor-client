/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.network.PacketUtils;
import net.minecraft.network.Packet;

import java.util.Set;

public class PacketBoolSettingScreen extends LeftRightListSettingScreen<Class<? extends Packet<?>>> {
    public PacketBoolSettingScreen(GuiTheme theme, Setting<Set<Class<? extends Packet<?>>>> setting) {
        super(theme, "Select Packets", setting, setting.get(), PacketUtils.REGISTRY);
    }

    @Override
    protected WWidget getValueWidget(Class<? extends Packet<?>> value) {
        return theme.label(getValueName(value));
    }

    @Override
    protected String getValueName(Class<? extends Packet<?>> value) {
        return PacketUtils.getName(value);
    }
}
