/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;

import java.util.List;
import java.util.Random;

public class AntiAFK extends Module {
    private final SettingGroup sgActions = settings.createGroup("actions");
    private final SettingGroup sgMessages = settings.createGroup("messages");

    // Actions

    private final Setting<Boolean> jump = sgActions.add(new BoolSetting.Builder()
        .name("jump")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swing = sgActions.add(new BoolSetting.Builder()
        .name("swing")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> sneak = sgActions.add(new BoolSetting.Builder()
        .name("sneak")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> sneakTime = sgActions.add(new IntSetting.Builder()
        .name("sneak-time")
        .defaultValue(5)
        .min(1)
        .sliderMin(1)
        .visible(sneak::get)
        .build()
    );

    private final Setting<Boolean> strafe = sgActions.add(new BoolSetting.Builder()
        .name("strafe")
        .defaultValue(false)
        .onChanged(aBoolean -> {
            strafeTimer = 0;
            direction = false;

            if (isActive()) {
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
            }
        })
        .build()
    );

    private final Setting<Boolean> spin = sgActions.add(new BoolSetting.Builder()
        .name("spin")
        .defaultValue(true)
        .build()
    );

    private final Setting<SpinMode> spinMode = sgActions.add(new EnumSetting.Builder<SpinMode>()
        .name("spin-mode")
        .defaultValue(SpinMode.Server)
        .visible(spin::get)
        .build()
    );

    private final Setting<Integer> spinSpeed = sgActions.add(new IntSetting.Builder()
        .name("speed")
        .defaultValue(7)
        .visible(spin::get)
        .build()
    );

    private final Setting<Integer> pitch = sgActions.add(new IntSetting.Builder()
        .name("pitch")
        .defaultValue(0)
        .range(-90, 90)
        .sliderRange(-90, 90)
        .visible(() -> spin.get() && spinMode.get() == SpinMode.Server)
        .build()
    );


    // Messages

    private final Setting<Boolean> sendMessages = sgMessages.add(new BoolSetting.Builder()
        .name("send-messages")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> randomMessage = sgMessages.add(new BoolSetting.Builder()
        .name("random")
        .defaultValue(false)
        .visible(sendMessages::get)
        .build()
    );

    private final Setting<Integer> delay = sgMessages.add(new IntSetting.Builder()
        .name("delay")
        .defaultValue(15)
        .min(0)
        .sliderMax(30)
        .visible(sendMessages::get)
        .build()
    );

    private final Setting<List<String>> messages = sgMessages.add(new StringListSetting.Builder()
        .name("messages")
        .defaultValue(
            "Meteor on top!",
            "Meteor on crack!"
        )
        .visible(sendMessages::get)
        .build()
    );

    public AntiAFK() {
        super(Categories.Player, "anti-afk");
    }

    private final Random random = new Random();
    private int messageTimer = 0;
    private int messageI = 0;
    private int sneakTimer = 0;
    private int strafeTimer = 0;
    private boolean direction = false;
    private float lastYaw;

    @Override
    public void onActivate() {
        if (sendMessages.get() && messages.get().isEmpty()) {
            warning("Message list is empty, disabling messages...");
            sendMessages.set(false);
        }

        lastYaw = mc.player.getYaw();
        messageTimer = delay.get() * 20;
    }

    @Override
    public void onDeactivate() {
        if (strafe.get()) {
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        // Jump
        if (jump.get()) {
            if (mc.options.jumpKey.isPressed()) mc.options.jumpKey.setPressed(false);
            else if (random.nextInt(99) == 0) mc.options.jumpKey.setPressed(true);
        }

        // Swing
        if (swing.get() && random.nextInt(99) == 0) {
            mc.player.swingHand(mc.player.getActiveHand());
        }

        // Sneak
        if (sneak.get()) {
            if (sneakTimer++ >= sneakTime.get()) {
                mc.options.sneakKey.setPressed(false);
                if (random.nextInt(99) == 0) sneakTimer = 0; // Sneak after ~5 seconds
            } else mc.options.sneakKey.setPressed(true);
        }

        // Strafe
        if (strafe.get() && strafeTimer-- <= 0) {
            mc.options.leftKey.setPressed(!direction);
            mc.options.rightKey.setPressed(direction);
            direction = !direction;
            strafeTimer = 20;
        }

        // Spin
        if (spin.get()) {
            lastYaw += spinSpeed.get();
            switch (spinMode.get()) {
                case Client -> mc.player.setYaw(lastYaw);
                case Server -> Rotations.rotate(lastYaw, pitch.get(), -15);
            }
        }

        // Messages
        if (sendMessages.get() && !messages.get().isEmpty() && messageTimer-- <= 0) {
            if (randomMessage.get()) messageI = random.nextInt(messages.get().size());
            else if (++messageI >= messages.get().size()) messageI = 0;

            ChatUtils.sendPlayerMsg(messages.get().get(messageI));
            messageTimer = delay.get() * 20;
        }
    }

    public enum SpinMode {
        Server,
        Client
    }
}
