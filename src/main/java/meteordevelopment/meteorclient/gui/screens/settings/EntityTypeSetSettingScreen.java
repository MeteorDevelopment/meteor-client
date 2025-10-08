/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.base.GroupedSetSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.EntityTypeSetSetting;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;

public class EntityTypeSetSettingScreen extends GroupedSetSettingScreen<EntityType<?>, EntityTypeSetSetting> {

    public EntityTypeSetSettingScreen(GuiTheme theme, EntityTypeSetSetting setting) {
        super(theme, "Select Entities", setting, EntityTypeSetSetting.GROUPS, Registries.ENTITY_TYPE);
    }

    @Override
    protected boolean includeValue(EntityType<?> value) {
        return setting.getFilter().test(value);
    }

    @Override
    protected WWidget getValueWidget(EntityType<?> value) {
        return theme.label(Names.get(value)).color(includeValue(value) ? theme.textColor() : theme.textSecondaryColor());
    }

    protected String[] getValueNames(EntityType<?> value) {
        return new String[]{
            Names.get(value),
            Registries.ENTITY_TYPE.getId(value).toString()
        };
    }
}
