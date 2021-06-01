/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.misc;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.world.Timer;
import minegame159.meteorclient.utils.world.TickRate;

public class TPSSync extends Module {
    public TPSSync() {
        super(Categories.Misc, "tps-sync", "Syncs the clients TPS with the server's.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Modules.get().get(Timer.class).setOverride((TickRate.INSTANCE.getTickRate() >= 1 ? TickRate.INSTANCE.getTickRate() : 1) / 20);
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }
}