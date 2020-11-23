/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.events;

import minegame159.meteorclient.events.packets.SendPacketEvent;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

import java.util.function.Predicate;

public class EventFilters {
    public static Predicate<SendPacketEvent> sendAttackPacket = event -> event.packet instanceof PlayerInteractEntityC2SPacket && ((PlayerInteractEntityC2SPacket) event.packet).getType() == PlayerInteractEntityC2SPacket.InteractionType.ATTACK;
    public static Predicate<SendPacketEvent> sendHandSwingPacket = event -> event.packet instanceof HandSwingC2SPacket;
}
