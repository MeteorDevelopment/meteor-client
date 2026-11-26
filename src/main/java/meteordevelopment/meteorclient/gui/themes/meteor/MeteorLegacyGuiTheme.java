/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor;

import meteordevelopment.meteorclient.gui.screens.MeteorLegacyModulesScreen;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorLegacyModule;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorLegacyTopBar;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorLegacyWindow;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.screens.Screen;

public class MeteorLegacyGuiTheme extends MeteorGuiTheme {
    public MeteorLegacyGuiTheme() {
        super("Meteor Legacy");

        accentColor.set(new SettingColor(145, 61, 226));
        checkboxColor.set(new SettingColor(145, 61, 226));
        textSecondaryColor.set(new SettingColor(150, 150, 150));
        textHighlightColor.set(new SettingColor(45, 125, 245, 100));
        titleTextColor.set(new SettingColor(255, 255, 255));
        moduleBackground.set(new SettingColor(50, 50, 50));

        backgroundColor.setColors(
            new SettingColor(20, 20, 20, 200),
            new SettingColor(30, 30, 30, 200),
            new SettingColor(40, 40, 40, 200)
        );
        outlineColor.setColors(
            new SettingColor(0, 0, 0),
            new SettingColor(10, 10, 10),
            new SettingColor(20, 20, 20)
        );
        scrollbarColor.setColors(
            new SettingColor(30, 30, 30, 200),
            new SettingColor(40, 40, 40, 200),
            new SettingColor(50, 50, 50, 200)
        );
        sliderHandle.setColors(
            new SettingColor(130, 0, 255),
            new SettingColor(140, 30, 255),
            new SettingColor(150, 60, 255)
        );
        sliderLeft.set(new SettingColor(100, 35, 170));
        sliderRight.set(new SettingColor(50, 50, 50));

        moduleAlignment.set(AlignmentX.Center);
        categoryIcons.set(false);
        modulesHelpText.set(true);
    }

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WMeteorLegacyWindow(icon, title));
    }

    @Override
    public WWidget module(Module module, String title) {
        return w(new WMeteorLegacyModule(module, title));
    }

    @Override
    public WTopBar topBar() {
        return w(new WMeteorLegacyTopBar());
    }

    @Override
    public TabScreen modulesScreen() {
        return new MeteorLegacyModulesScreen(this);
    }

    @Override
    public boolean isModulesScreen(Screen screen) {
        return screen instanceof MeteorLegacyModulesScreen;
    }
}
