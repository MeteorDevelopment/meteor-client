/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

//Updated by squidoodly 24/07/2020

import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

public class MessageAura extends Module {
    private int messageI;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
            .name("messages")
            .description("The specified messages sent to the player.")
            .defaultValue("Meteor on Crack!")
            .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Will not send any messages to people friended.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> publicChat = sgGeneral.add(new BoolSetting.Builder()
            .name("public-chat")
            .description("The messages will be sent to public chat.")
            .defaultValue(false)
            .build()
    );

    public MessageAura() {
        super(Categories.Misc, "message-aura", "Sends a specified message to any player that enters render distance.");
    }

    @Override
    public void onActivate() {
        messageI = 0;
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof PlayerEntity) || event.entity.getUuid().equals(mc.player.getUuid())) return;

        int i;
        if (random.get()) {
            i = Utils.random(0, messages.get().size());
        } else {
            if (messageI >= messages.get().size()) messageI = 0;
            i = messageI++;
        }
        String text = messages.get().get(i);

        if (!ignoreFriends.get() || (ignoreFriends.get() && !Friends.get().isFriend((PlayerEntity)event.entity))) {
            if (!publicChat.get()){
                mc.player.sendChatMessage("/msg " + event.entity.getEntityName() + " " + text);
            } else {
                mc.player.sendChatMessage(text);
            }
        }
    }
}
