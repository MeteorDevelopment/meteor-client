/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.movement;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.mixin.EntityVelocityUpdateS2CPacketAccessor;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.events.packets.PacketEvent;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public class Velocity extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> knockback = sgGeneral.add(new BoolSetting.Builder()
            .name("knockback")
            .description("Modifies the amount of knockback you take from attacks.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Double> knockbackHorizontal = sgGeneral.add(new DoubleSetting.Builder()
            .name("knockback-horizontal")
            .description("How much horizontal knockback you will take.")
            .defaultValue(0)
            .sliderMin(0).sliderMax(1)
            .visible(knockback::get)
            .build()
    );

    public final Setting<Double> knockbackVertical = sgGeneral.add(new DoubleSetting.Builder()
            .name("knockback-vertical")
            .description("How much vertical knockback you will take.")
            .defaultValue(0)
            .sliderMin(0).sliderMax(1)
            .visible(knockback::get)
            .build()
    );

    public final Setting<Boolean> explosions = sgGeneral.add(new BoolSetting.Builder()
            .name("explosions")
            .description("Modifies your knockback from explosions.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Double> explosionsHorizontal = sgGeneral.add(new DoubleSetting.Builder()
            .name("explosions-horizontal")
            .description("How much velocity you will take from explosions horizontally.")
            .defaultValue(0)
            .sliderMin(0).sliderMax(1)
            .visible(explosions::get)
            .build()
    );

    public final Setting<Double> explosionsVertical = sgGeneral.add(new DoubleSetting.Builder()
            .name("explosions-vertical")
            .description("How much velocity you will take from explosions vertically.")
            .defaultValue(0)
            .sliderMin(0).sliderMax(1)
            .visible(explosions::get)
            .build()
    );

    public final Setting<Boolean> liquids = sgGeneral.add(new BoolSetting.Builder()
            .name("liquids")
            .description("Modifies the amount you are pushed by flowing liquids.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Double> liquidsHorizontal = sgGeneral.add(new DoubleSetting.Builder()
            .name("liquids-horizontal")
            .description("How much velocity you will take from liquids horizontally.")
            .defaultValue(0)
            .sliderMin(0).sliderMax(1)
            .visible(liquids::get)
            .build()
    );

    public final Setting<Double> liquidsVertical = sgGeneral.add(new DoubleSetting.Builder()
            .name("liquids-vertical")
            .description("How much velocity you will take from liquids vertically.")
            .defaultValue(0)
            .sliderMin(0).sliderMax(1)
            .visible(liquids::get)
            .build()
    );

    public final Setting<Boolean> entityPush = sgGeneral.add(new BoolSetting.Builder()
            .name("entity-push")
            .description("Modifies the amount you are pushed by entities.")
            .defaultValue(true)
            .build()
    );

    public final Setting<Double> entityPushAmount = sgGeneral.add(new DoubleSetting.Builder()
            .name("entity-push-amount")
            .description("How much you will be pushed.")
            .defaultValue(0)
            .sliderMin(0).sliderMax(1)
            .visible(entityPush::get)
            .build()
    );

    public final Setting<Boolean> blocks = sgGeneral.add(new BoolSetting.Builder()
            .name("blocks")
            .description("Prevents you from being pushed out of blocks.")
            .defaultValue(true)
            .build()
    );

    public Velocity() {
        super(Categories.Movement, "velocity", "Prevents you from being moved by external forces.");
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (knockback.get() && event.packet instanceof EntityVelocityUpdateS2CPacket && ((EntityVelocityUpdateS2CPacket) event.packet).getId() == mc.player.getEntityId()) {
            EntityVelocityUpdateS2CPacket packet = (EntityVelocityUpdateS2CPacket) event.packet;
            double velX = (packet.getVelocityX() / 8000d - mc.player.getVelocity().x) * knockbackHorizontal.get();
            double velY = (packet.getVelocityY() / 8000d - mc.player.getVelocity().y) * knockbackVertical.get();
            double velZ = (packet.getVelocityZ() / 8000d - mc.player.getVelocity().z) * knockbackHorizontal.get();
            ((EntityVelocityUpdateS2CPacketAccessor) packet).setX((int) (velX * 8000 + mc.player.getVelocity().x * 8000));
            ((EntityVelocityUpdateS2CPacketAccessor) packet).setY((int) (velY * 8000 + mc.player.getVelocity().y * 8000));
            ((EntityVelocityUpdateS2CPacketAccessor) packet).setZ((int) (velZ * 8000 + mc.player.getVelocity().z * 8000));
        }
    }

    public double getHorizontal(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }
    public double getVertical(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }

}