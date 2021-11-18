/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

public class Spam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay between specified messages in ticks.")
            .defaultValue(20)
            .min(0)
            .sliderMax(200)
            .build()
    );

    private final Setting<Boolean> random = sgGeneral.add(new BoolSetting.Builder()
            .name("randomise")
            .description("Selects a random message from your spam message list.")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
            .name("messages")
            .description("Messages to use for spam.")
            .build()
    );

    private final Setting<Boolean> antiSpamBypass = sgGeneral.add(new BoolSetting.Builder()
            .name("anti-spam-bypass")
            .description("Add random text at the bottom of the text")
            .defaultValue(false)
            .build());

    private int messageI, timer;

    public Spam() {
        super(Categories.Misc, "spam", "Spams specified messages in chat.");
    }

    @Override
    public void onActivate() {
        timer = delay.get();
        messageI = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (messages.get().isEmpty()) return;

        if (timer <= 0) {
            int i;
            if (random.get()) {
                i = Utils.random(0, messages.get().size());
            } else {
                if (messageI >= messages.get().size()) messageI = 0;
                i = messageI++;
            }

            String text = messages.get().get(i);
            if (antiSpamBypass.get()) {
                /*
                 * This has to be lower-case to avoid anti-capital-letter plugin
                 * Also, set the length to 16 because the max length of player name is 16,
                 * which means this random text can be utilized to input random (invalid) player names for exploits
                 */
                text += RandomStringUtils.randomAlphabetic(16).toLowerCase();
            }
            mc.player.sendChatMessage(text);
            timer = delay.get();
        } else {
            timer--;
        }
    }
}
