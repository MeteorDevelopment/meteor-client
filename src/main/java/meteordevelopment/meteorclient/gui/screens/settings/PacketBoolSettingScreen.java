/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.base.CollectionListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Predicate;

public class PacketBoolSettingScreen extends CollectionListSettingScreen<PacketType<? extends @NotNull Packet<?>>> {
    public PacketBoolSettingScreen(GuiTheme theme, Setting<Set<PacketType<? extends @NotNull Packet<?>>>> setting) {
        super(theme, "Select Packets", setting, setting.get(), PacketUtils.getPackets());
    }

    @Override
    protected boolean includeValue(PacketType<? extends @NotNull Packet<?>> value) {
        Predicate<PacketType<? extends @NotNull Packet<?>>> filter = ((PacketListSetting) setting).filter;

        if (filter == null) return true;
        return filter.test(value);
    }

    @Override
    protected WWidget getValueWidget(PacketType<? extends @NotNull Packet<?>> value) {
        return theme.label(value.toString());
    }

    @Override
    protected String[] getValueNames(PacketType<? extends @NotNull Packet<?>> value) {
        return new String[]{
            value.toString()
        };
    }
}
