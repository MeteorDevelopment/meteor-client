/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.nametags;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.entity.EntityType;

public class NametagsEntityTypeDataScreen extends WindowScreen {
    private final NametagsEntityTypeData entityTypeData;
    private final EntityType entityType;
    private final EntityTypeDataSetting<NametagsEntityTypeData> setting;
    private Settings settings;
    private WContainer settingsContainer;

    public NametagsEntityTypeDataScreen(GuiTheme guiTheme, NametagsEntityTypeData entityData, EntityType entityType, EntityTypeDataSetting<NametagsEntityTypeData> setting) {
        super(guiTheme, "Entity Data");

        this.entityTypeData = entityData;
        this.entityType = entityType;
        this.setting = setting;
    }

    @Override
    public void initWidgets() {
        settings = new Settings();
        SettingGroup sgGeneral = settings.getDefaultGroup();

        sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale of the nametag.")
            .defaultValue(1.1)
            .onModuleActivated(settingDoubleSetting -> settingDoubleSetting.set(entityTypeData.scale))
            .onChanged(settingDouble -> {
                entityTypeData.scale = settingDouble;
                changed(entityTypeData, entityType, setting);
            })
            .min(0.1)
            .build()
        );

        sgGeneral.add(new BoolSetting.Builder()
            .name("through-walls")
            .description("Renders nametags through walls.")
            .defaultValue(false)
            .onModuleActivated(settingBooleanSetting -> settingBooleanSetting.set(entityTypeData.throughWalls))
            .onChanged(settingBoolean -> {
                entityTypeData.throughWalls = settingBoolean;
                changed(entityTypeData, entityType, setting);
            })
            .build()
        );

        final Setting<Boolean> culling = sgGeneral.add(new BoolSetting.Builder()
            .name("culling")
            .description("Only render a certain number of nametags at a certain distance.")
            .defaultValue(false)
            .onModuleActivated(settingBooleanSetting -> settingBooleanSetting.set(entityTypeData.culling))
            .onChanged(settingBoolean -> {
                entityTypeData.culling = settingBoolean;
                changed(entityTypeData, entityType, setting);
            })
            .build()
        );

        sgGeneral.add(new DoubleSetting.Builder()
            .name("culling-range")
            .description("Only render nametags within this distance of your player.")
            .defaultValue(20)
            .min(0)
            .sliderMax(200)
            .visible(culling::get)
            .onModuleActivated(settingDoubleSetting -> entityTypeData.maxCullRange = settingDoubleSetting.get())
            .onChanged(settingDouble -> {
                entityTypeData.maxCullRange = settingDouble;
                changed(entityTypeData, entityType, setting);
            })
            .build()
        );

        sgGeneral.add(new IntSetting.Builder()
            .name("culling-count")
            .description("Only render this many nametags.")
            .defaultValue(50)
            .min(1)
            .sliderRange(1, 100)
            .visible(culling::get)
            .onModuleActivated(settingIntegerSetting -> entityTypeData.maxCullCount = settingIntegerSetting.get())
            .onChanged(settingInteger -> {
                entityTypeData.maxCullCount = settingInteger;
                changed(entityTypeData, entityType, setting);
            })
            .build()
        );

        settings.onActivated();
        settingsContainer = (WContainer) add(theme.settings(settings)).expandX().widget();
    }

    private void changed(NametagsEntityTypeData entityTypeData, EntityType entityType, EntityTypeDataSetting<NametagsEntityTypeData> setting) {
        if (!entityTypeData.isChanged() && entityType != null && setting != null) {
            setting.get().put(entityType, entityTypeData);
            setting.onChanged();
        }
        if (settingsContainer != null) {
            settings.tick(settingsContainer, theme);
        }

        entityTypeData.changed();
    }
}
