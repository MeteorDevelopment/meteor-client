/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientboundSetEntityMotionPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

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
        .sliderMax(1)
        .visible(knockback::get)
        .build()
    );

    public final Setting<Double> knockbackVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("knockback-vertical")
        .description("How much vertical knockback you will take.")
        .defaultValue(0)
        .sliderMax(1)
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
        .sliderMax(1)
        .visible(explosions::get)
        .build()
    );

    public final Setting<Double> explosionsVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("explosions-vertical")
        .description("How much velocity you will take from explosions vertically.")
        .defaultValue(0)
        .sliderMax(1)
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
        .sliderMax(1)
        .visible(liquids::get)
        .build()
    );

    public final Setting<Double> liquidsVertical = sgGeneral.add(new DoubleSetting.Builder()
        .name("liquids-vertical")
        .description("How much velocity you will take from liquids vertically.")
        .defaultValue(0)
        .sliderMax(1)
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
        .sliderMax(1)
        .visible(entityPush::get)
        .build()
    );

    public final Setting<Boolean> blocks = sgGeneral.add(new BoolSetting.Builder()
        .name("blocks")
        .description("Prevents you from being pushed out of blocks.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> sinking = sgGeneral.add(new BoolSetting.Builder()
        .name("sinking")
        .description("Prevents you from sinking in liquids.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> fishing = sgGeneral.add(new BoolSetting.Builder()
        .name("fishing")
        .description("Prevents you from being pulled by fishing rods.")
        .defaultValue(false)
        .build()
    );

    public Velocity() {
        super(Categories.Movement, "velocity", "Prevents you from being moved by external forces.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!sinking.get()) return;
        if (mc.options.keyJump.isDown() || mc.options.keyShift.isDown()) return;

        if ((mc.player.isInWater() || mc.player.isInLava()) && mc.player.getDeltaMovement().y < 0) {
            ((IVec3) mc.player.getDeltaMovement()).meteor$setY(0);
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (knockback.get() && event.packet instanceof ClientboundSetEntityMotionPacket packet
            && packet.id() == mc.player.getId()) {
            double velX = (packet.movement().x() - mc.player.getDeltaMovement().x) * knockbackHorizontal.get();
            double velY = (packet.movement().y() - mc.player.getDeltaMovement().y) * knockbackVertical.get();
            double velZ = (packet.movement().z() - mc.player.getDeltaMovement().z) * knockbackHorizontal.get();
            ((ClientboundSetEntityMotionPacketAccessor) (Object) packet).meteor$setMovement(
                new Vec3(velX + mc.player.getDeltaMovement().x, velY + mc.player.getDeltaMovement().y, velZ + mc.player.getDeltaMovement().z)
            );
        }
    }

    public double getHorizontal(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }

    public double getVertical(Setting<Double> setting) {
        return isActive() ? setting.get() : 1;
    }
}
