/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.hud.elements;

import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.hud.DoubleTextHudElement;
import minegame159.meteorclient.systems.hud.ElementRegister;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.player.NameProtect;
import minegame159.meteorclient.utils.render.color.SettingColor;

@ElementRegister(name = "welcome")
public class WelcomeHud extends DoubleTextHudElement {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("Color of welcome text.")
            .defaultValue(new SettingColor(120, 43, 153))
            .build()
    );

    public WelcomeHud() {
        super("welcome", "Displays a welcome message.", "Welcome to Meteor Client, ");
        rightColor = color.get();
    }

    @Override
    protected String getRight() {
        return Modules.get().get(NameProtect.class).getName(mc.getSession().getUsername()) + "!";
    }
}
