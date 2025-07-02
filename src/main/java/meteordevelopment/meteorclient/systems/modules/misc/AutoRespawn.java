/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.WaypointsModule;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screen.DeathScreen;

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

    private final Setting<Integer> respawnMessageDelay = sgGeneral.add(new IntSetting.Builder()
        .name("message-delay")
        .description("The delay before sending respawn message in ticks.")
        .defaultValue(1)
        .sliderMax(100)
        .visible(sendRespawnMessage::get)
        .build()
    );

    // Fields
    private int respawnTimer;
    private boolean respawning = false;

    public AutoRespawn() {
        super(Categories.Player, "auto-respawn", "Automatically respawns after death.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onOpenScreenEvent(OpenScreenEvent event) {
        if (!(event.screen instanceof DeathScreen)) return;

        Modules.get().get(WaypointsModule.class).addDeath(mc.player.getPos());
        mc.player.requestRespawn();
        event.cancel();

        if (!sendRespawnMessage.get()) return;
        respawnTimer = 0;
        respawning = true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!respawning) return;
        respawnTimer++;

        if (respawnTimer <= respawnMessageDelay.get()) return;
        respawning = false;

        info("Sending message '"+respawnMessage.get()+"'");
        ChatUtils.sendPlayerMsg(respawnMessage.get());
    }
}
