/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.PacketBoolSetting;
import minegame159.meteorclient.utils.network.PacketUtils;
import net.minecraft.network.Packet;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PacketBoolSettingScreen extends WindowScreen {
    private final PacketBoolSetting setting;
    private final WTextBox filter;

    private String filterText = "";

    public PacketBoolSettingScreen(PacketBoolSetting setting) {
        super("Select Packets", true);

        this.setting = setting;

        // Filter
        filter = new WTextBox("", 200);
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.getText().trim();

            clear();
            initWidgets();
        };

        initWidgets();
    }

    private void initWidgets() {
        add(filter).fillX().expandX();
        row();

        List<Class<? extends Packet<?>>> packets = new ArrayList<>(setting.get().keySet());
        packets.sort(Comparator.comparing(PacketUtils::getName));

        WTable left = add(new WTable()).expandX().fillX().top().getWidget();
        add(new WVerticalSeparator()).expandY();
        WTable right = add(new WTable()).expandX().fillX().top().getWidget();

        for (Class<? extends Packet<?>> packet : packets) {
            String name = PacketUtils.getName(packet);
            if (!StringUtils.containsIgnoreCase(name, filterText)) continue;

            if (setting.get().getBoolean(packet)) {
                right.add(new WLabel(name));
                right.add(new WMinus()).getWidget().action = () -> {
                    setting.get().put(packet, false);
                    setting.changed();
                    clear();
                    initWidgets();
                };

                right.row();
            } else {
                left.add(new WLabel(name));
                left.add(new WPlus()).getWidget().action = () -> {
                    setting.get().put(packet, true);
                    setting.changed();
                    clear();
                    initWidgets();
                };

                left.row();
            }
        }
    }
}
