/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.OpenScreenEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.client.gui.screen.DeathScreen;

public class AutoRespawn extends ToggleModule {
    public AutoRespawn() {
        super(Category.Player, "auto-respawn", "Automatically respawns.");
    }

    @EventHandler
    private final Listener<OpenScreenEvent> onOpenScreenEvent = new Listener<>(event -> {
        if (!(event.screen instanceof DeathScreen)) return;

        mc.player.requestRespawn();
        event.cancel();
    });
}
