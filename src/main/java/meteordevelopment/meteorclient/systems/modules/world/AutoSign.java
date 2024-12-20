/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.mixin.AbstractSignEditScreenAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;

public class AutoSign extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> line1 = sgGeneral.add(new StringSetting.Builder()
        .name("Line 1")
        .description("Line 1")
        .defaultValue("Meteor")
        .build()
    );

    private final Setting<String> line2 = sgGeneral.add(new StringSetting.Builder()
        .name("Line 2")
        .description("Line 2")
        .defaultValue("Client")
        .build()
    );

    private final Setting<String> line3 = sgGeneral.add(new StringSetting.Builder()
        .name("Line 3")
        .description("Line 3")
        .defaultValue("on")
        .build()
    );

    private final Setting<String> line4 = sgGeneral.add(new StringSetting.Builder()
        .name("Line 4")
        .description("Line 4")
        .defaultValue("Crack!")
        .build()
    );

    private final Setting<Boolean> front = sgGeneral.add(new BoolSetting.Builder()
        .name("Front")
        .description("Place text on front or back of sign")
        .defaultValue(true)
        .build()
    );

    public AutoSign() {
        super(Categories.World, "auto-sign", "Automatically writes signs.");
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (!(event.screen instanceof AbstractSignEditScreen)) return;

        SignBlockEntity sign = ((AbstractSignEditScreenAccessor) event.screen).getSign();

        mc.player.networkHandler.sendPacket(new UpdateSignC2SPacket(sign.getPos(), front.get(), line1.get(), line2.get(), line3.get(), line4.get()));

        event.cancel();
    }
}
