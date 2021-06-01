/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

public class Sneak extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> packet = sgGeneral.add(new BoolSetting.Builder()
            .name("packet")
            .description("Sneaks using packets")
            .defaultValue(false)
            .build()
    );

    public Sneak() {
        super (Categories.Movement, "sneak", "Sneaks for you");
    }

    @Override
    public void onDeactivate() {
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        mc.options.keySneak.setPressed(false);
        mc.player.setSneaking(false);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (packet.get()) {
            if (mc.options.keySneak.isPressed()) mc.options.keySneak.setPressed(false);
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        } else {
            mc.options.keySneak.setPressed(true);
        }
    }


}
