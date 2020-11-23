/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

//Created by squidoodly 16/07/2020 (Yay! Empty class!!!)

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.AttackEntityEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import net.minecraft.entity.player.PlayerEntity;

public class AntiFriendHit extends ToggleModule {
    public AntiFriendHit() {
        super(Category.Combat, "anti-friend-hit", "Cancels attacks that hit friends");
    }

    @EventHandler
    private final Listener<AttackEntityEvent> onAttackEntity = new Listener<>(event -> {
        if (event.entity instanceof PlayerEntity &&  ModuleManager.INSTANCE.get(AntiFriendHit.class).isActive() && !FriendManager.INSTANCE.attack((PlayerEntity) event.entity)) event.cancel();
    });
}
