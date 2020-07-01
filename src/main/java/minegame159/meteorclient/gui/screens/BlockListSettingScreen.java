package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.function.Consumer;

public class BlockListSettingScreen extends WindowScreen {
    private final Setting<List<Block>> setting;
    private final WTextBox filter;

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
        Consumer<Block> blockForEach = block -> {
            if (block == Blocks.AIR || setting.get().contains(block)) return;

            table1.add(new WItemWithLabel(block.asItem().getStackForRender(), block.getName().getString()));

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
        };

        // Sort all blocks
        if (filter.text.isEmpty()) {
            Registry.BLOCK.forEach(blockForEach);
        } else {
            List<Pair<Block, Integer>> blocks = new ArrayList<>();
            Registry.BLOCK.forEach(block -> {
                int words = Utils.search(block.getName().getString(), filter.text);
                if (words > 0) blocks.add(new Pair<>(block, words));
            });
            blocks.sort(Comparator.comparingInt(value -> -value.getRight()));
            for (Pair<Block, Integer> pair : blocks) blockForEach.accept(pair.getLeft());
        }

        if (table1.getCells().size() > 0) add(new WVerticalSeparator()).expandY();

        // Selected blocks
        WTable table2 = add(new WTable()).top().getWidget();
        for (Block block : setting.get()) {
            table2.add(new WItemWithLabel(block.asItem().getStackForRender(), block.getName().getString()));

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
