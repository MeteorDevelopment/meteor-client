/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.player.Player;

public class PlayerLoadNotifier extends Module {
    public PlayerLoadNotifier() {
        super(Categories.Misc, "player-load-notifier", "Notifies you when another player loads into your client world.");
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof Player player) || mc.player == null || player == mc.player) return;

        info("%s loaded.", player.getName().getString());
    }
}
