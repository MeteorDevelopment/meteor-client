/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.misc.TPSSync;

public class Timer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> multiplier = sgGeneral.add(new DoubleSetting.Builder()
            .name("multiplier")
            .description("The timer multiplier amount.")
            .defaultValue(1)
            .min(0.1)
            .sliderMin(1)
            .build()
    );

    public static final double OFF = 1;
    private double override = 1;

    public Timer() {
        super(Categories.World, "timer", "Changes the speed of everything in your game.");
    }

    public double getMultiplier() {
        return override != OFF ? override : (isActive() ? multiplier.get() : OFF);
    }

    public void setOverride(double override) {
        this.override = override;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        if (Modules.get().get(TPSSync.class).isActive()) {
            WHorizontalList list = theme.horizontalList();
            list.add(theme.label("Multiplier is overwritten by TPSSync."));
            WButton disableBtn = list.add(theme.button("Disable TPSSync")).widget();
            disableBtn.action = () -> {
                TPSSync tpsSync = Modules.get().get(TPSSync.class);
                if (tpsSync.isActive()) tpsSync.toggle();
                list.visible = false;
            };
            return list;
        }
        return null;
    }
}
