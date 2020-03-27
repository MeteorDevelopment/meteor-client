package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.gui.PanelListScreen;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class BlockListSettingScreen extends PanelListScreen {
    private Setting<List<Block>> setting;
    private WTextBox filter;

    public BlockListSettingScreen(Setting<List<Block>> setting) {
        super("Select blocks");

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

        // All blocks
        WVerticalList list = hList.add(new WVerticalList(4));
        list.add(filter);

        WGrid grid1 = list.add(new WGrid(4, 4, 2));
        Registry.BLOCK.forEach(block -> {
            if (block == Blocks.AIR || setting.get().contains(block)) return;

            WItemWithLabel item = new WItemWithLabel(block.asItem().getStackForRender());
            if (!filter.text.isEmpty()) {
                if (!StringUtils.containsIgnoreCase(item.getLabelText(), filter.text)) return;
            }

            WPlus plus = new WPlus();
            plus.action = () -> {
                if (!setting.get().contains(block)) {
                    setting.get().add(block);
                    setting.changed();
                    clear();
                    initWidgets();
                }
            };

            grid1.addRow(item, plus);
        });

        // Selected blocks
        WGrid grid2 = hList.add(new WGrid(4, 4, 2));
        for (Block block : setting.get()) {
            WItemWithLabel item = new WItemWithLabel(block.asItem().getStackForRender());

            WMinus minus = new WMinus();
            minus.action = () -> {
                if (setting.get().remove(block)) {
                    setting.changed();
                    clear();
                    initWidgets();
                }
            };

            grid2.addRow(item, minus);
        }

        layout();
    }
}
