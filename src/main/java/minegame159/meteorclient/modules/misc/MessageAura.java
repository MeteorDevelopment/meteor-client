/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

//Updated by squidoodly 24/07/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.EntityAddedEvent;
import minegame159.meteorclient.friends.Friend;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.settings.StringSetting;
import net.minecraft.entity.player.PlayerEntity;

public class MessageAura extends ToggleModule {
    public MessageAura() {
        super(Category.Misc, "message-aura", "Sends a message to every player when they enter render distance.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
            .name("message")
            .description("The message sent to players.")
            .defaultValue("Meteor on Crack!").build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Doesn't send messages to friends.")
            .defaultValue(false)
            .build()
    );

    @EventHandler
    private final Listener<EntityAddedEvent> onEntityAdded = new Listener<>(event -> {
        if (!(event.entity instanceof PlayerEntity) || event.entity.getUuid().equals(mc.player.getUuid())) return;

        if (!ignoreFriends.get() || (ignoreFriends.get() && !FriendManager.INSTANCE.contains(new Friend((PlayerEntity)event.entity)))) {
            mc.player.sendChatMessage("/msg " + ((PlayerEntity) event.entity).getGameProfile().getName() + " " + message.get());
        }
    });
}
