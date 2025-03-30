/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.WaypointsModule;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screen.DeathScreen;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class AutoRespawn extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> sendRespawnMessage = sgGeneral.add(new BoolSetting.Builder()
        .name("respawn-message")
        .description("Sends a message or command upon respawning.")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> respawnMessage = sgGeneral.add(new StringSetting.Builder()
        .name("message")
        .description("The message/command to send.")
        .defaultValue("Meteor on Crack!")
        .visible(sendRespawnMessage::get)
        .build()
    );

    private final Setting<Double> respawnMessageDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("initial-delay")
        .description("The delay before sending respawn message in seconds.")
        .defaultValue(0.5)
        .sliderMax(10)
        .visible(sendRespawnMessage::get)
        .build()
    );

    public AutoRespawn() {
        super(Categories.Player, "auto-respawn", "Automatically respawns after death.");
    }

    private void sendDeathMessage() {
        long delay = (long) Math.floor(respawnMessageDelay.get() * 1000);
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ChatUtils.sendPlayerMsg(respawnMessage.get());
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onOpenScreenEvent(OpenScreenEvent event) {
        if (!(event.screen instanceof DeathScreen)) return;

        Modules.get().get(WaypointsModule.class).addDeath(mc.player.getPos());
        mc.player.requestRespawn();
        event.cancel();

        if (!sendRespawnMessage.get()) return;
        // execute on a different thread to not yield literally everything.
        MeteorExecutor.execute(() -> sendDeathMessage());
    }
}
