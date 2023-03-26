/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Spam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> fileSpam = sgGeneral.add(new BoolSetting.Builder()
        .name("file-spam")
        .description("Use files instead of message lists.")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> textPath = sgGeneral.add(new StringSetting.Builder()
        .name("text-path")
        .defaultValue(new File(MeteorClient.FOLDER, "spam.txt").getAbsolutePath())
        .visible(() -> false)
        .build()
    );

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
        .name("messages")
        .description("Messages to use for spam.")
        .defaultValue(List.of("Meteor on Crack!"))
        .visible(() -> !fileSpam.get())
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between specified messages in ticks.")
        .defaultValue(20)
        .min(0)
        .sliderMax(200)
        .build()
    );

    private final Setting<Boolean> disableOnLeave = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-leave")
        .description("Disables spam when you leave a server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-disconnect")
        .description("Disables spam when you are disconnected from a server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> random = sgGeneral.add(new BoolSetting.Builder()
        .name("randomize")
        .description("Selects a random message from your spam message list.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder()
        .name("bypass")
        .description("Add random text at the end of the message to try to bypass anti spams.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> length = sgGeneral.add(new IntSetting.Builder()
        .name("length")
        .description("Number of characters used to bypass anti spam.")
        .visible(bypass::get)
        .defaultValue(16)
        .sliderRange(1, 256)
        .build()
    );

    private List<String> lines;
    private int messageI, timer;

    public Spam() {
        super(Categories.Misc, "spam", "Spams specified messages in chat.");
    }

    @Override
    public void onActivate() {
        if (fileSpam.get()) {
            try {
                lines = Files.readAllLines(Path.of(textPath.get()));
            } catch (IOException e) {
                error("No file selected, please select a file in the GUI.");
                toggle();
            }
        }

        timer = delay.get();
        messageI = 0;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        return Utils.fileSelectWidget(textPath, theme);
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnLeave.get()) toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (lines == null || lines.isEmpty()) {
            error("The bookbot file is empty or not found. (%s)", textPath.get());
            toggle();
            return;
        }

        if (timer <= 0) {
            List<String> msgs = fileSpam.get() ? lines : messages.get();
            int i;
            if (random.get()) {
                i = Utils.random(0, msgs.size());
            } else {
                if (messageI >= msgs.size()) messageI = 0;
                i = messageI++;
            }

            String text = msgs.get(i);
            if (bypass.get()) {
                text += " " + RandomStringUtils.randomAlphabetic(length.get()).toLowerCase();
            }

            ChatUtils.sendPlayerMsg(text);
            timer = delay.get();
        } else {
            timer--;
        }
    }
}
