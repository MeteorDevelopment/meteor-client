/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render.hud.modules;

import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.render.hud.HUD;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class TimeHud extends DoubleTextHudElement {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<TimeType> timeType = sgGeneral.add(new EnumSetting.Builder<TimeType>()
            .name("use-time")
            .description("Which time to use.")
            .defaultValue(TimeType.World)
            .build()
    );

    public TimeHud(HUD hud) {
        super(hud, "time", "Displays the world time.", "Time: ");
    }

    @Override
    protected String getRight() {
        String time = "00:00";

        switch (timeType.get()) {
            case World:
                if (isInEditor()) return time;

                int ticks = (int) (mc.world.getTimeOfDay() % 24000);
                ticks += 6000;
                if (ticks > 24000) ticks -= 24000;
                time = String.format("%02d:%02d", ticks / 1000, (int) (ticks % 1000 / 1000.0 * 60));
                break;
            case Local:
                time = LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
                break;
        }

        return time;
    }

    public enum TimeType {
        World,
        Local
    }
}
