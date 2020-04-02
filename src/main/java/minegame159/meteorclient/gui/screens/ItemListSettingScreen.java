package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.Alignment;
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
        super("Select items");

        this.setting = setting;

        // Filter
        filter = new WTextBox("", 200);
        filter.boundingBox.fullWidth = true;
        filter.setFocused(true);
        filter.action = textBox -> {
            clear();
            initWidgets();
        };

        initWidgets();
    }

    private void initWidgets() {
        WHorizontalList hList = add(new WHorizontalList(4, Alignment.X.Center, Alignment.Y.Top));

        // All items
        WVerticalList list = hList.add(new WVerticalList(4));
        list.add(filter);

        WGrid grid1 = list.add(new WGrid(4, 4, 2));
        Registry.ITEM.forEach(item -> {
            if (item == Items.AIR || setting.get().contains(item)) return;

            WItemWithLabel wItem = new WItemWithLabel(item.getStackForRender());
            if (!filter.text.isEmpty()) {
                if (!StringUtils.containsIgnoreCase(wItem.getLabelText(), filter.text)) return;
            }

            WPlus plus = new WPlus();
            plus.action = () -> {
                if (!setting.get().contains(item)) {
                    setting.get().add(item);
                    setting.changed();
                    clear();
                    initWidgets();
                }
            };

            grid1.addRow(wItem, plus);
        });

        // Selected blocks
        WGrid grid2 = hList.add(new WGrid(4, 4, 2));
        for (Item item : setting.get()) {
            WItemWithLabel wItem = new WItemWithLabel(item.getStackForRender());

            WMinus minus = new WMinus();
            minus.action = () -> {
                if (setting.get().remove(item)) {
                    setting.changed();
                    clear();
                    initWidgets();
                }
            };

            grid2.addRow(wItem, minus);
        }

        layout();
    }
}
