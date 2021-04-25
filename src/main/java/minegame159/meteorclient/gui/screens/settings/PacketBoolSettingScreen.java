/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.WindowScreen;
import minegame159.meteorclient.gui.utils.Cell;
import minegame159.meteorclient.gui.widgets.containers.WHorizontalList;
import minegame159.meteorclient.gui.widgets.containers.WTable;
import minegame159.meteorclient.gui.widgets.input.WTextBox;
import minegame159.meteorclient.gui.widgets.pressable.WPressable;
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
    private WHorizontalList list;

    private String filterText = "";

    public PacketBoolSettingScreen(GuiTheme theme, PacketBoolSetting setting) {
        super(theme, "Select packets");

        this.setting = setting;

        filter = add(theme.textBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();

            list.clear();
            initWidgets();
        };

        list = add(theme.horizontalList()).expandX().widget();

        initWidgets();
    }

    private void initWidgets() {
        List<Class<? extends Packet<?>>> packets = new ArrayList<>(setting.get().keySet());
        packets.sort(Comparator.comparing(PacketUtils::getName));

        Cell<WTable> leftCell = list.add(theme.table()).top();
        WTable left = leftCell.widget();

        list.add(theme.verticalSeparator()).expandWidgetY();

        Cell<WTable> rightCell = list.add(theme.table()).top();
        WTable right = rightCell.widget();

        for (Class<? extends Packet<?>> packet : packets) {
            String name = PacketUtils.getName(packet);
            if (!StringUtils.containsIgnoreCase(name, filterText)) continue;

            if (setting.get().getBoolean(packet)) {
                widget(right, packet, name, false);
            }
            else {
                widget(left, packet, name, true);
            }
        }

        if (left.cells.size() > 0) leftCell.expandX();
        if (right.cells.size() > 0) rightCell.expandX();
    }

    private void widget(WTable table, Class<? extends Packet<?>> packet, String name, boolean add) {
        table.add(theme.label(name)).expandCellX();

        WPressable button = table.add(add ? theme.plus() : theme.minus()).widget();
        button.action = () -> {
            if (add) setting.get().put(packet, true);
            else setting.get().removeBoolean(packet);
            setting.changed();

            list.clear();
            initWidgets();
        };

        table.row();
    }
}
