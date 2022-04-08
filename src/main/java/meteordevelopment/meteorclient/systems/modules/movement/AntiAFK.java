/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AntiAFK extends Module {
    public enum SpinMode {
        Server,
        Client
    }

    private final SettingGroup sgActions = settings.createGroup("Actions");
    private final SettingGroup sgMessages = settings.createGroup("Messages");

    // Actions

    private final Setting<Boolean> spin = sgActions.add(new BoolSetting.Builder()
        .name("spin")
        .description("Spins.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SpinMode> spinMode = sgActions.add(new EnumSetting.Builder<SpinMode>()
        .name("spin-mode")
        .description("The method of rotating.")
        .defaultValue(SpinMode.Server)
        .visible(spin::get)
        .build()
    );

    private final Setting<Integer> spinSpeed = sgActions.add(new IntSetting.Builder()
        .name("spin-speed")
        .description("The speed to spin you.")
        .defaultValue(7)
        .visible(spin::get)
        .build()
    );

    private final Setting<Double> pitch = sgActions.add(new DoubleSetting.Builder()
        .name("pitch")
        .description("The pitch to set in server mode.")
        .defaultValue(-90)
        .range(-90, 90)
        .sliderRange(-90, 90)
        .visible(() -> spin.get() && spinMode.get() == SpinMode.Server)
        .build()
    );

    private final Setting<Boolean> jump = sgActions.add(new BoolSetting.Builder()
        .name("jump")
        .description("Jumps.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> click = sgActions.add(new BoolSetting.Builder()
        .name("click")
        .description("Clicks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> disco = sgActions.add(new BoolSetting.Builder()
        .name("disco")
        .description("Sneaks and unsneaks quickly.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> strafe = sgActions.add(new BoolSetting.Builder()
        .name("strafe")
        .description("Strafe right and left")
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

    // Messages

    private final Setting<Boolean> sendMessages = sgMessages.add(new BoolSetting.Builder()
        .name("send-messages")
        .description("Sends messages to prevent getting kicked for AFK.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> delay = sgMessages.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between specified messages in seconds.")
        .defaultValue(2)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> randomMessage = sgMessages.add(new BoolSetting.Builder()
        .name("random")
        .description("Selects a random message from your message list.")
        .defaultValue(false)
        .build()
    );

    private final List<String> messages = new ArrayList<>();
    private int timer;
    private int messageI;
    private int strafeTimer = 0;
    private boolean direction = false;

    private final Random random = new Random();

    private float prevYaw;

    public AntiAFK() {
        super(Categories.Player, "anti-afk", "Performs different actions to prevent getting kicked for AFK reasons.");
    }

    @Override
    public void onActivate() {
        prevYaw = mc.player.getYaw();
        timer = delay.get() * 20;
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
        if (Utils.canUpdate()) {
            //Spin
            if (spin.get()) {
                prevYaw += spinSpeed.get();
                switch (spinMode.get()) {
                    case Client -> mc.player.setYaw(prevYaw);
                    case Server -> Rotations.rotate(prevYaw, pitch.get(), -15, null);
                }
            }

            //Jump
            if (jump.get() && mc.options.jumpKey.isPressed()) mc.options.jumpKey.setPressed(false);
            if (jump.get() && mc.options.sneakKey.isPressed()) mc.options.sneakKey.setPressed(false);
            else if (jump.get() && random.nextInt(99) + 1 == 50) mc.options.jumpKey.setPressed(true);

            //Click
            if (click.get() && random.nextInt(99) + 1 == 45) {
                mc.options.attackKey.setPressed(true);
                Utils.leftClick();
                mc.options.attackKey.setPressed(false);
            }

            //Disco
            if (disco.get() && random.nextInt(24) + 1 == 15) mc.options.sneakKey.setPressed(true);

            //Spam
            if (sendMessages.get() && !messages.isEmpty())
                if (timer <= 0) {
                    int i;
                    if (randomMessage.get()) {
                        i = Utils.random(0, messages.size());
                    } else {
                        if (messageI >= messages.size()) messageI = 0;
                        i = messageI++;
                    }

                    mc.player.sendChatMessage(messages.get(i));

                    timer = delay.get() * 20;
                } else {
                    timer--;
                }

            //Strafe
            if (strafe.get() && strafeTimer == 20) {
                mc.options.leftKey.setPressed(!direction);
                mc.options.rightKey.setPressed(direction);
                direction = !direction;
                strafeTimer = 0;
            } else
                strafeTimer++;
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        messages.removeIf(String::isEmpty);

        WTable table = theme.table();
        fillTable(theme, table);

        return table;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        table.add(theme.horizontalSeparator("Message List")).expandX();

        // Messages
        for (int i = 0; i < messages.size(); i++) {
            int msgI = i;
            String message = messages.get(i);

            WTextBox textBox = table.add(theme.textBox(message)).minWidth(100).expandX().widget();
            textBox.action = () -> messages.set(msgI, textBox.get());

            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                messages.remove(msgI);

                table.clear();
                fillTable(theme, table);
            };

            table.row();
        }

        // New Message
        WPlus add = table.add(theme.plus()).expandCellX().right().widget();
        add.action = () -> {
            messages.add("");

            table.clear();
            fillTable(theme, table);
        };
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();

        messages.removeIf(String::isEmpty);
        NbtList messagesTag = new NbtList();

        for (String message : messages) messagesTag.add(NbtString.of(message));
        tag.put("messages", messagesTag);

        return tag;
    }

    @Override
    public Module fromTag(NbtCompound tag) {
        messages.clear();

        if (tag.contains("messages")) {
            NbtList messagesTag = tag.getList("messages", 8);
            for (NbtElement messageTag : messagesTag) messages.add(messageTag.asString());
        } else {
            messages.add("This is an AntiAFK message. Meteor on Crack!");
        }

        return super.fromTag(tag);
    }
}
