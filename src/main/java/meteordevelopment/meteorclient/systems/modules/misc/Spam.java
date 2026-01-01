/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;
import java.util.Random;

public class Spam extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
        .name("messages")
        .description("Messages to use for spam.")
        .defaultValue(List.of("Meteor on Crack!"))
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
        .name("randomise")
        .description("Selects a random message from your spam message list.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoSplitMessages = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-split-messages")
        .description("Automatically split up large messages after a certain length")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> splitLength = sgGeneral.add(new IntSetting.Builder()
        .name("split-length")
        .description("The length after which to split messages in chat")
        .visible(autoSplitMessages::get)
        .defaultValue(256)
        .min(1)
        .sliderMax(256)
        .build()
    );

    private final Setting<Integer> autoSplitDelay = sgGeneral.add(new IntSetting.Builder()
        .name("split-delay")
        .description("The delay between split messages in ticks.")
        .visible(autoSplitMessages::get)
        .defaultValue(20)
        .min(0)
        .sliderMax(200)
        .build()
    );

    private final Setting<Boolean> variations = sgGeneral.add(new BoolSetting.Builder()
        .name("variations")
        .description("Randomly modifies messages to bypass anti-spam detection.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder()
        .name("bypass")
        .description("Add random text at the end of the message to try to bypass anti spams.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> uppercase = sgGeneral.add(new BoolSetting.Builder()
        .name("include-uppercase-characters")
        .description("Whether the bypass text should include uppercase characters.")
        .visible(bypass::get)
        .defaultValue(true)
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

    private int messageI, timer, splitNum;
    private String text;
    private final Random rng = new Random();

    public Spam() {
        super(Categories.Misc, "spam", "Spams specified messages in chat.");
    }

    private String applyVariations(String message) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            
            // Skip spaces for most variations
            if (c == ' ') {
                result.append(c);
                continue;
            }
            
            // Apply balanced variations - good mix of changes and readability
            switch (rng.nextInt(8)) {
                case 0: // Random case (frequent)
                    if (Character.isLetter(c)) {
                        result.append(rng.nextBoolean() ? Character.toUpperCase(c) : Character.toLowerCase(c));
                    } else {
                        result.append(c);
                    }
                    break;
                    
                case 1: // Leet speak substitution (moderate)
                    result.append(leetSpeak(c));
                    break;
                    
                case 2: // Add invisible character (rare)
                    if (rng.nextInt(15) == 0) {
                        result.append(c).append('\u200B'); // Zero-width space
                    } else {
                        result.append(c);
                    }
                    break;
                    
                case 3: // Replace with similar looking character (moderate)
                    result.append(similarChar(c));
                    break;
                    
                case 4: // Add extra space (rare)
                    if (rng.nextInt(20) == 0 && i < message.length() - 1 && message.charAt(i + 1) != ' ') {
                        result.append(c).append(' ');
                    } else {
                        result.append(c);
                    }
                    break;
                    
                default: // No change (37.5% chance)
                    result.append(c);
                    break;
            }
        }
        
        return result.toString();
    }
    
    private char leetSpeak(char c) {
        switch (Character.toLowerCase(c)) {
            case 'a': return rng.nextInt(2) == 0 ? (rng.nextBoolean() ? '4' : '@') : c;
            case 'e': return rng.nextInt(2) == 0 ? '3' : c;
            case 'i': return rng.nextInt(3) == 0 ? '1' : c;
            case 'o': return rng.nextInt(2) == 0 ? '0' : c;
            case 's': return rng.nextInt(3) == 0 ? (rng.nextBoolean() ? '5' : '$') : c;
            case 't': return rng.nextInt(3) == 0 ? '7' : c;
            case 'l': return rng.nextInt(4) == 0 ? '1' : c;
            case 'g': return rng.nextInt(5) == 0 ? '9' : c;
            case 'b': return rng.nextInt(5) == 0 ? '8' : c;
            default: return c;
        }
    }
    
    private char similarChar(char c) {
        switch (Character.toLowerCase(c)) {
            case 'a': return rng.nextInt(4) == 0 ? (rng.nextBoolean() ? 'á' : 'à') : c;
            case 'e': return rng.nextInt(4) == 0 ? (rng.nextBoolean() ? 'é' : 'è') : c;
            case 'i': return rng.nextInt(5) == 0 ? (rng.nextBoolean() ? 'í' : 'ì') : c;
            case 'o': return rng.nextInt(4) == 0 ? (rng.nextBoolean() ? 'ó' : 'ò') : c;
            case 'u': return rng.nextInt(6) == 0 ? (rng.nextBoolean() ? 'ú' : 'ù') : c;
            case 'n': return rng.nextInt(8) == 0 ? 'ñ' : c;
            case 'c': return rng.nextInt(10) == 0 ? 'ç' : c;
            default: return c;
        }
    }

    @Override
    public void onActivate() {
        timer = delay.get();
        messageI = 0;
        splitNum = 0;
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
        if (messages.get().isEmpty()) return;

        if (timer <= 0) {
            if (text == null) {
                int i;
                if (random.get()) {
                    i = Utils.random(0, messages.get().size());
                } else {
                    if (messageI >= messages.get().size()) messageI = 0;
                    i = messageI++;
                }

                text = messages.get().get(i);
                
                // Apply variations if enabled
                if (variations.get()) {
                    text = applyVariations(text);
                }
                
                if (bypass.get()) {
                    String bypass = RandomStringUtils.insecure().nextAlphabetic(length.get());
                    if (!uppercase.get()) bypass = bypass.toLowerCase();

                    text += " " + bypass;
                }
            }

            if (autoSplitMessages.get() && text.length() > splitLength.get()) {
                // the number of individual messages the whole text needs to be broken into
                double length = text.length();
                int splits = (int) Math.ceil(length / splitLength.get());

                // determine which chunk we need to send
                int start = splitNum * splitLength.get();
                int end = Math.min(start + splitLength.get(), text.length());
                ChatUtils.sendPlayerMsg(text.substring(start, end));

                splitNum = ++splitNum % splits;
                timer = autoSplitDelay.get();
                if (splitNum == 0) { // equals zero when all chunks are sent
                    timer = delay.get();
                    text = null;
                }
            } else {
                if (text.length() > 256) text = text.substring(0, 256); // prevent kick
                ChatUtils.sendPlayerMsg(text);
                timer = delay.get();
                text = null;
            }
        } else {
            timer--;
        }
    }
}
