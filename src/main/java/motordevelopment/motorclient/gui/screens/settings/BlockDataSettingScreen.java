/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.screens.settings;

import motordevelopment.motorclient.gui.GuiTheme;
import motordevelopment.motorclient.gui.WindowScreen;
import motordevelopment.motorclient.gui.renderer.GuiRenderer;
import motordevelopment.motorclient.gui.widgets.containers.WTable;
import motordevelopment.motorclient.gui.widgets.input.WTextBox;
import motordevelopment.motorclient.gui.widgets.pressable.WButton;
import motordevelopment.motorclient.settings.BlockDataSetting;
import motordevelopment.motorclient.settings.IBlockData;
import motordevelopment.motorclient.utils.misc.IChangeable;
import motordevelopment.motorclient.utils.misc.ICopyable;
import motordevelopment.motorclient.utils.misc.ISerializable;
import motordevelopment.motorclient.utils.misc.Names;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static motordevelopment.motorclient.MotorClient.mc;

public class BlockDataSettingScreen extends WindowScreen {
    private static final List<Block> BLOCKS = new ArrayList<>(100);

    private final BlockDataSetting<?> setting;

    private WTable table;
    private String filterText = "";

    public BlockDataSettingScreen(GuiTheme theme, BlockDataSetting<?> setting) {
        super(theme, "Configure Blocks");

        this.setting = setting;
    }

    @Override
    public void initWidgets() {
        WTextBox filter = add(theme.textBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();

            table.clear();
            initTable();
        };

        table = add(theme.table()).expandX().widget();

        initTable();
    }

    public <T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> void initTable() {
        for (Block block : Registries.BLOCK) {
            T blockData = (T) setting.get().get(block);

            if (blockData != null && blockData.isChanged()) BLOCKS.addFirst(block);
            else BLOCKS.add(block);
        }

        for (Block block : BLOCKS) {
            String name = Names.get(block);
            if (!StringUtils.containsIgnoreCase(name, filterText)) continue;

            T blockData = (T) setting.get().get(block);

            table.add(theme.itemWithLabel(block.asItem().getDefaultStack(), Names.get(block))).expandCellX();
            table.add(theme.label((blockData != null && blockData.isChanged()) ? "*" : " "));

            WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
            edit.action = () -> {
                T data = blockData;
                if (data == null) data = (T) setting.defaultData.get().copy();

                mc.setScreen(data.createScreen(theme, block, (BlockDataSetting<T>) setting));
            };

            WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
            reset.action = () -> {
                setting.get().remove(block);
                setting.onChanged();

                if (blockData != null && blockData.isChanged()) {
                    table.clear();
                    initTable();
                }
            };

            table.row();
        }

        BLOCKS.clear();
    }
}
