package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.PacketBoolSetting;
import minegame159.meteorclient.utils.PacketUtils;
import net.minecraft.network.Packet;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PacketBoolSettingScreen extends WindowScreen {
    private final PacketBoolSetting setting;
    private final WTextBox filter;

    public PacketBoolSettingScreen(PacketBoolSetting setting) {
        super("Select Packets", true);

        this.setting = setting;

        // Filter
        filter = new WTextBox("", 200);
        filter.setFocused(true);
        filter.action = textBox -> {
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
            if (!StringUtils.containsIgnoreCase(name, filter.text)) continue;

            if (setting.get().getBoolean(packet)) {
                right.add(new WLabel(name));
                right.add(new WMinus()).getWidget().action = wMinus -> {
                    setting.get().put(packet, false);
                    setting.changed();
                    clear();
                    initWidgets();
                };

                right.row();
            } else {
                left.add(new WLabel(name));
                left.add(new WPlus()).getWidget().action = wPlus -> {
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
