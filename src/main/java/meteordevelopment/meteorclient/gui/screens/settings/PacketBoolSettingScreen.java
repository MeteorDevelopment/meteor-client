/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;

import java.util.Set;

public class PacketBoolSettingScreen extends RegistryListSettingScreen<PacketType<? extends Packet<?>>> {
    public PacketBoolSettingScreen(GuiTheme theme, Setting<Set<PacketType<? extends Packet<?>>>> setting) {
        super(theme, "Select Packets", setting, setting.get(), PacketUtils.REGISTRY);
    }

    @Override
    protected boolean includeValue(PacketType<? extends Packet<?>> value) {
        return ((PacketListSetting) setting).filter(value);
    }

    @Override
    protected WWidget getValueWidget(PacketType<? extends Packet<?>> value) {
        return theme.label(getValueName(value));
    }

    @Override
    protected String getValueName(PacketType<? extends Packet<?>> value) {
        return PacketUtils.getName(value);
    }
}
