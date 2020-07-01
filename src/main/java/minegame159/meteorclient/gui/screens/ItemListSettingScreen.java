package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.function.Consumer;

public class ItemListSettingScreen extends WindowScreen {
    private final Setting<List<Item>> setting;
    private final WTextBox filter;

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
        Consumer<Item> itemForEach = item -> {
            if (item == Items.AIR || setting.get().contains(item)) return;

            table1.add(new WItemWithLabel(item.getStackForRender()));

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
        };

        // Sort all items
        if (filter.text.isEmpty()) {
            Registry.ITEM.forEach(itemForEach);
        } else {
            List<Pair<Item, Integer>> items = new ArrayList<>();
            Registry.ITEM.forEach(item -> {
                int words = Utils.search(item.getName().getString(), filter.text);
                if (words > 0) items.add(new Pair<>(item, words));
            });
            items.sort(Comparator.comparingInt(value -> -value.getRight()));
            for (Pair<Item, Integer> pair : items) itemForEach.accept(pair.getLeft());
        }

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
