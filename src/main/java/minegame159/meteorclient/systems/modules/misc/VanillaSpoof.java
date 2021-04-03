/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.misc;

import io.netty.buffer.Unpooled;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.mixin.CustomPayloadC2SPacketAccessor;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;

public class VanillaSpoof extends Module {
    public VanillaSpoof() {
        super(Categories.Misc, "vanilla-spoof", "When connecting to a server it spoofs the client name to be 'vanilla'.");

        MeteorClient.EVENT_BUS.subscribe(new Listener());
    }

    private class Listener {
        private boolean skip = false;

        @EventHandler
        private void onPacketSend(PacketEvent.Send event) {
            if (!isActive() || !(event.packet instanceof CustomPayloadC2SPacket) || skip) return;
            Identifier id = ((CustomPayloadC2SPacketAccessor) event.packet).getChannel();

            if (id.equals(CustomPayloadC2SPacket.BRAND)) {
                event.cancel();

                skip = true;
                MeteorClient.LOG.info("Spoofed vanilla client");
                mc.getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(CustomPayloadC2SPacket.BRAND, new PacketByteBuf(Unpooled.buffer()).writeString("vanilla")));
                skip = false;
            }
        }
    }
}
