/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.EntityTypeSetting;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.entity.EntityType;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

public class EntityTypeSettingScreen extends WindowScreen {
    private final EntityTypeSetting setting;

    private WTable table;

    private WTextBox filter;
    private String filterText = "";

    public EntityTypeSettingScreen(GuiTheme theme, EntityTypeSetting setting) {
        super(theme, "Select Entity");

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
        for (EntityType<?> entityType : Registry.ENTITY_TYPE) {
            if (setting.onlyAttackable && !EntityUtils.isAttackable(entityType)) continue;
            if (setting.onlyLiving && !EntityUtils.isLiving(entityType)) continue;

            if (!filterText.isEmpty() && !StringUtils.containsIgnoreCase(Names.get(entityType), filterText)) continue;
            table.add(theme.label(Names.get(entityType)));

            WButton select = table.add(theme.button("Select")).expandCellX().right().widget();
            select.action = () -> {
                setting.set(entityType);
                setting.onChanged();
                onClose();
            };

            table.row();
        }
    }
}
