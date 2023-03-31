/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.hit.EntityHitResult;

public class VehicleOneHit extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
        .name("amount")
        .description("The number of packets to send.")
        .defaultValue(16)
        .range(1, 100)
        .sliderRange(1, 20)
        .build()
    );

    private boolean ignorePackets;

    public VehicleOneHit() {
        super(Categories.Player, "vehicle-one-hit", "Destroy vehicles with one hit.");
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (ignorePackets
            || !(event.packet instanceof PlayerInteractEntityC2SPacket)
            || !(mc.crosshairTarget instanceof EntityHitResult ehr)
            || (!(ehr.getEntity() instanceof AbstractMinecartEntity) && !(ehr.getEntity() instanceof BoatEntity))
        ) return;

        ignorePackets = true;
        for (int i = 0; i < amount.get() - 1; i++) {
            mc.player.networkHandler.sendPacket(event.packet);
        }
        ignorePackets = false;
    }
}
