/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.movement;

import motordevelopment.motorclient.events.motor.KeyEvent;
import motordevelopment.motorclient.events.world.TickEvent;
import motordevelopment.motorclient.settings.BoolSetting;
import motordevelopment.motorclient.settings.Setting;
import motordevelopment.motorclient.settings.SettingGroup;
import motordevelopment.motorclient.systems.modules.Categories;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.render.Freecam;
import motordevelopment.motorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;

public class AirJump extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> maintainLevel = sgGeneral.add(new BoolSetting.Builder()
        .name("maintain-level")
        .description("Maintains your current Y level when holding the jump key.")
        .defaultValue(false)
        .build()
    );

    private int level;

    public AirJump() {
        super(Categories.Movement, "air-jump", "Lets you jump in the air.");
    }

    @Override
    public void onActivate() {
        level = mc.player.getBlockPos().getY();
    }

    @EventHandler
    private void onKey(KeyEvent event) {
        if (Modules.get().isActive(Freecam.class) || mc.currentScreen != null || mc.player.isOnGround()) return;

        if (event.action != KeyAction.Press) return;

        if (mc.options.jumpKey.matchesKey(event.key, 0)) {
            level = mc.player.getBlockPos().getY();
            mc.player.jump();
        }
        else if (mc.options.sneakKey.matchesKey(event.key, 0)) {
            level--;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (Modules.get().isActive(Freecam.class) || mc.player.isOnGround()) return;

        if (maintainLevel.get() && mc.player.getBlockPos().getY() == level && mc.options.jumpKey.isPressed()) {
            mc.player.jump();
        }
    }
}
