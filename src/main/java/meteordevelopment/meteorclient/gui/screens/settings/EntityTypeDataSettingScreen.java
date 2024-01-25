/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.EntityTypeDataSetting;
import meteordevelopment.meteorclient.settings.IEntityTypeData;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import meteordevelopment.meteorclient.utils.misc.ICopyable;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EntityTypeDataSettingScreen extends WindowScreen {
    private final List<EntityType> ENTITY_TYPES = new ArrayList<>(100);

    private final EntityTypeDataSetting<?> setting;

    private WTable table;
    private String filterText = "";

    public EntityTypeDataSettingScreen(GuiTheme theme, EntityTypeDataSetting<?> setting) {
        super(theme, "Configure Entity Types");

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

    public <T extends ICopyable<T> & ISerializable<T> & IChangeable & IEntityTypeData<T>> void initTable() {
        for (EntityType entityType : Registries.ENTITY_TYPE) {
            T entityTypeData = (T) setting.get().get(entityType);

            if (entityTypeData != null) ENTITY_TYPES.add(0, entityType);
            else ENTITY_TYPES.add(entityType);
        }

        for (EntityType entityType : ENTITY_TYPES) {
            String name = Names.get(entityType);
            if (!StringUtils.containsIgnoreCase(name, filterText)) continue;

            T entityTypeData = (T) setting.get().get(entityType);

            table.add(theme.label(name)).expandCellX();
            table.add(theme.label(entityTypeData != null && entityTypeData.isChanged() ? "*": " "));

            WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
            edit.action = () -> {
                T data = entityTypeData;
                if (data == null) data = (T) setting.defaultData.get().copy();

                mc.setScreen(data.createScreen(theme, entityType, (EntityTypeDataSetting<T>) setting));
            };

            WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
            reset.action = () -> {
                setting.get().remove(entityType);
                setting.onChanged();

                if (entityTypeData != null && entityTypeData.isChanged()) {
                    table.clear();
                    initTable();
                }
            };

            table.row();
        }

        ENTITY_TYPES.clear();
    }
}
