/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.movement;

//Created by squidoodly 10/07/2020

import motordevelopment.motorclient.events.world.TickEvent;
import motordevelopment.motorclient.mixin.ClientPlayerEntityAccessor;
import motordevelopment.motorclient.mixininterface.IHorseBaseEntity;
import motordevelopment.motorclient.settings.BoolSetting;
import motordevelopment.motorclient.settings.Setting;
import motordevelopment.motorclient.settings.SettingGroup;
import motordevelopment.motorclient.systems.modules.Categories;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractHorseEntity;

public class EntityControl extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> maxJump = sgGeneral.add(new BoolSetting.Builder()
        .name("max-jump")
        .description("Sets jump power to maximum.")
        .defaultValue(true)
        .build()
    );

    public EntityControl() {
        super(Categories.Movement, "entity-control", "Lets you control rideable entities without a saddle.");
    }

    @Override
    public void onDeactivate() {
        if (!Utils.canUpdate() || mc.world.getEntities() == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof AbstractHorseEntity) ((IHorseBaseEntity) entity).motor$setSaddled(false);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof AbstractHorseEntity) ((IHorseBaseEntity) entity).motor$setSaddled(true);
        }

        if (maxJump.get()) ((ClientPlayerEntityAccessor) mc.player).setMountJumpStrength(1);
    }
}
