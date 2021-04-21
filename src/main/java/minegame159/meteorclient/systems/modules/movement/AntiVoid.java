/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.utils.Utils;

public class AntiVoid extends Module {

    private final SettingGroup sgDefault = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgDefault.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The method to prevent you from falling into the void.")
            .defaultValue(Mode.Jump)
            .onChanged(a -> onActivate())
            .build()
    );

    private boolean wasFlightEnabled, hasRun;

    public AntiVoid() {
        super(Categories.Movement, "anti-void", "Attempts to prevent you from falling into the void.");
    }

    @Override
    public void onActivate() {
        if (mode.get() == Mode.Flight) wasFlightEnabled = Modules.get().isActive(Flight.class);
    }

    @Override
    public void onDeactivate() {
        if (!wasFlightEnabled && mode.get() == Mode.Flight && Utils.canUpdate() && Modules.get().isActive(Flight.class)) {
            Modules.get().get(Flight.class).toggle();
        }
    }

    @EventHandler
    public void onPreTick(TickEvent.Pre event) {
        if (mc.player.getY() > 0 || mc.player.getY() < -15) {
            if (hasRun && mode.get() == Mode.Flight && Modules.get().isActive(Flight.class)) {
                Modules.get().get(Flight.class).toggle();
                hasRun = false;
            }
            return;
        }

        switch (mode.get()) {
            case Flight:
                if (!Modules.get().isActive(Flight.class)) Modules.get().get(Flight.class).toggle();
                hasRun = true;
                break;
            case Jump:
                mc.player.jump();
                break;
        }
    }

    public enum Mode {
        Flight,
        Jump
    }

}
