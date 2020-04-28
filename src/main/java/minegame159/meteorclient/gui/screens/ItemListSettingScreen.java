package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ItemListSettingScreen extends WindowScreen {
    private Setting<List<Item>> setting;
    private WTextBox filter;

    public ItemListSettingScreen(Setting<List<Item>> setting) {
        super("Select items", true);

        this.setting = setting;

        // Filter
        filter = new WTextBox("", 0);
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

        // All items
        WTable table1 = add(new WTable()).top().getWidget();
        Registry.ITEM.forEach(item -> {
            if (item == Items.AIR || setting.get().contains(item)) return;

            WItemWithLabel wItem = new WItemWithLabel(item.getStackForRender());
            if (!filter.text.isEmpty()) {
                if (!StringUtils.containsIgnoreCase(wItem.getLabelText(), filter.text)) return;
            }
            table1.add(wItem);

            WPlus plus = table1.add(new WPlus()).getWidget();
            plus.action = plus1 -> {
                if (!setting.get().contains(item)) {
                    setting.get().add(item);
                    setting.changed();
                    clear();
                    initWidgets();
                }
            };

            table1.row();
        });

        if (table1.getCells().size() > 0) add(new WVerticalSeparator()).expandY();

        // Selected blocks
        WTable table2 = add(new WTable()).top().getWidget();
        for (Item item : setting.get()) {
            table2.add(new WItemWithLabel(item.getStackForRender()));

            WMinus minus = table2.add(new WMinus()).getWidget();
            minus.action = minus1 -> {
                if (setting.get().remove(item)) {
                    setting.changed();
                    clear();
                    initWidgets();
                }
            };

            table2.row();
        }
    }
}
