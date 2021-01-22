/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class AutoSprint extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> permanent = sgGeneral.add(new BoolSetting.Builder()
            .name("permanent")
            .description("Makes you still sprint even if you do not move.")
            .defaultValue(true)
            .build()
    );

    public AutoSprint() {
        super(Category.Movement, "auto-sprint", "Automatically sprints.");
    }
    
    @Override
    public void onDeactivate() {
        mc.player.setSprinting(false);
    }

    @EventHandler
    private final Listener<TickEvent.Post> onTick = new Listener<>(event -> {
        if(mc.player.forwardSpeed > 0 && !permanent.get()) {
            mc.player.setSprinting(true);
        } else if (permanent.get()) {
            mc.player.setSprinting(true);
        }
    });
}
