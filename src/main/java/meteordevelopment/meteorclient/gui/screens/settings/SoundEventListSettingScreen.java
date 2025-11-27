/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.base.CollectionListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;

import java.util.List;

public class SoundEventListSettingScreen extends CollectionListSettingScreen<SoundEvent> {
    public SoundEventListSettingScreen(GuiTheme theme, Setting<List<SoundEvent>> setting) {
        super(theme, "Select Sounds", setting, setting.get(), Registries.SOUND_EVENT);
    }

    @Override
    protected WWidget getValueWidget(SoundEvent value) {
        return theme.label(value.id().getPath());
    }

    @Override
    protected String[] getValueNames(SoundEvent value) {
        return new String[]{
            value.id().toString(),
            I18n.translate("subtitles." + value.id().getPath())
        };
    }
}
