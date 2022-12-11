/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WItemWithLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BlockSetting;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import org.apache.commons.lang3.StringUtils;

public class BlockSettingScreen extends WindowScreen {
    private final BlockSetting setting;

    private WTable table;

    private WTextBox filter;
    private String filterText = "";

    public BlockSettingScreen(GuiTheme theme, BlockSetting setting) {
        super(theme, "Select Block");

        this.setting = setting;
    }

    @Override
    public void initWidgets() {
        filter = add(theme.textBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();

            table.clear();
            initTable();
        };

        table = add(theme.table()).expandX().widget();

        initTable();
    }

    private void initTable() {
        for (Block block : Registries.BLOCK) {
            if (setting.filter != null && !setting.filter.test(block)) continue;
            if (skipValue(block)) continue;

            WItemWithLabel item = theme.itemWithLabel(block.asItem().getDefaultStack(), Names.get(block));
            if (!filterText.isEmpty() && !StringUtils.containsIgnoreCase(item.getLabelText(), filterText)) continue;
            table.add(item);

            WButton select = table.add(theme.button("Select")).expandCellX().right().widget();
            select.action = () -> {
                setting.set(block);
                close();
            };

            table.row();
        }
    }

    protected boolean skipValue(Block value) {
        return value == Blocks.AIR || Registries.BLOCK.getId(value).getPath().endsWith("_wall_banner");
    }
}
