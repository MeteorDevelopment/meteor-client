/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.MiddleMouseButtonEvent;
import minegame159.meteorclient.friends.Friend;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.entity.player.PlayerEntity;

public class MiddleClickFriend extends ToggleModule {
    public MiddleClickFriend() {
        super(Category.Misc, "middle-click-friend", "Adds/removes player as friend.");
    }

    @EventHandler
    private final Listener<MiddleMouseButtonEvent> onMiddleMouseButton = new Listener<>(event -> {
        if (mc.currentScreen != null) return;
        if (mc.targetedEntity instanceof PlayerEntity) FriendManager.INSTANCE.addOrRemove(new Friend((PlayerEntity) mc.targetedEntity));
    });
}
