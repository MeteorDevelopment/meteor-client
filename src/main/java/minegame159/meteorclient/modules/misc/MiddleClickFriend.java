/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.meteor.MiddleMouseButtonEvent;
import minegame159.meteorclient.friends.Friend;
import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import net.minecraft.entity.player.PlayerEntity;

public class MiddleClickFriend extends Module {
    public MiddleClickFriend() {
        super(Categories.Misc, "middle-click-friend", "Adds or removes a player as a friend via middle click.");
    }

    @EventHandler
    private void onMiddleMouseButton(MiddleMouseButtonEvent event) {
        if (mc.currentScreen != null) return;
        if (mc.targetedEntity instanceof PlayerEntity) Friends.get().addOrRemove(new Friend((PlayerEntity) mc.targetedEntity));
    }
}
