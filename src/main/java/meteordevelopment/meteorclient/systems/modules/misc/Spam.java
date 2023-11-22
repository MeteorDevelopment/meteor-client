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

    // PMspam button
    private final Setting<Boolean> PMspam = sgGeneral.add(new BoolSetting.Builder()
        .name("PMspam")
        .description("Parses playersList and /msg them")
        .defaultValue(false)
        .build()
    );


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
    private void onScreenOpen(OpenScreenEvent event) {
        if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnLeave.get()) toggle();
    }


    /**
     * Iterator through player list<br>
     * Declared globally to save its value<br><br>
     * *Will be reset if (it > playerNames.length)
     */
    int it = 0;


    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (messages.get().isEmpty()) return;

        if (timer <= 0) {
            int i;
            if (random.get()) {
                i = Utils.random(0, messages.get().size());
            }
            else {
                if (messageI >= messages.get().size()) messageI = 0;
                i = messageI++;
            }


            /* This var will be reassigned and outputted at the end
             * The struct looks like this:
             * /msg + playerNames[it] + text + bypass
             */
            String text = messages.get().get(i);

            // PMspam REALIZATION   ///////////////////////////////////////////////////////////////////////////////////////
            // If our button is toggled         // TODO: List caching
            if (PMspam.get()) {
                if(mc.player != null) {
                    // Var to store String[] of names
                    String[] playerNames = new String[0];

                    // if LOCAL SERVER
                    if (mc.getServer() != null && mc.isConnectedToLocalServer()) {
                        playerNames = mc.getServer().getPlayerNames();
                    }
                    // if MULTIPLAYER
                    else if (mc.player.getEntityWorld().getPlayers() != null) {
                        playerNames = mc.player.networkHandler.getCommandSource().getPlayerNames().toArray(new String[0]);
                    }
                    // to avoid out of bound
                    if (playerNames.length > 0) {
                        if (it >= playerNames.length) { it = 0; }

                        // Exclude our name // TODO: Friends name (&& if NPC????)
                        if (playerNames[it].equals(mc.player.getEntityName()) && playerNames.length != 1) {
                            it++;
                        }

                        if (it < playerNames.length) {
                            text = "/msg " + playerNames[it] + " " + text;
                            it++;
                        }
                    }
                }
            }
            // PMspam END OF REALIZATION    ///////////////////////////////////////////////////////////////////////////////


            if (bypass.get()) {
                text += " " + RandomStringUtils.randomAlphabetic(length.get()).toLowerCase();
            }

            ChatUtils.sendPlayerMsg(text);
            timer = delay.get();
        }
        else {
            timer--;
        }
    }
}
