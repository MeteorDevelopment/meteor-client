/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

public class BlockSettingScreen extends WindowScreen {
    private final Setting<Block> setting;
    private final WTextBox filter;

    private String filterText = "";

    public BlockSettingScreen(Setting<Block> setting) {
        super("Select Block", true);

        this.setting = setting;

        // Filter
        filter = new WTextBox("", 400);
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.getText().trim();

            clear();
            initWidgets();
        };

        initWidgets();
    }

    private void initWidgets() {
        add(filter).fillX().expandX();
        row();

        // Blocks
        WTable table = add(new WTable()).getWidget();
        for (Block block : Registry.BLOCK) {
            WItemWithLabel item = new WItemWithLabel(block.asItem().getDefaultStack());
            if (!filterText.isEmpty()) {
                if (!StringUtils.containsIgnoreCase(item.getLabelText(), filterText)) continue;
            }
            table.add(item);

            WButton select = table.add(new WButton("Select")).getWidget();
            select.action = () -> {
                setting.set(block);
                onClose();
            };
            table.add(new WHorizontalSeparator());
        }
    }
}
