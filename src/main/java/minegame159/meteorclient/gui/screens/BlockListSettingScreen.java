package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class BlockListSettingScreen extends WindowScreen {
    private Setting<List<Block>> setting;
    private WTextBox filter;

    public BlockListSettingScreen(Setting<List<Block>> setting) {
        super("Select blocks", true);

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

        // All blocks
        WTable table1 = add(new WTable()).top().getWidget();
        Registry.BLOCK.forEach(block -> {
            if (block == Blocks.AIR || setting.get().contains(block)) return;

            WItemWithLabel item = new WItemWithLabel(block.asItem().getStackForRender());
            if (!filter.text.isEmpty()) {
                if (!StringUtils.containsIgnoreCase(item.getLabelText(), filter.text)) return;
            }
            table1.add(item);

            WPlus plus = table1.add(new WPlus()).getWidget();
            plus.action = plus1 -> {
                if (!setting.get().contains(block)) {
                    setting.get().add(block);
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
        for (Block block : setting.get()) {
            table2.add(new WItemWithLabel(block.asItem().getStackForRender()));

            WMinus minus = table2.add(new WMinus()).getWidget();
            minus.action = minus1 -> {
                if (setting.get().remove(block)) {
                    setting.changed();
                    clear();
                    initWidgets();
                }
            };

            table2.row();
        }
    }
}
