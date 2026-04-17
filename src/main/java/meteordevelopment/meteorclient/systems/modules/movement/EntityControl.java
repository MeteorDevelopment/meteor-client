/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.EntityMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.JumpingMount;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EntityControl extends Module {
    private final SettingGroup sgControl = settings.createGroup("Control");
    private final SettingGroup sgSpeed = settings.createGroup("Speed");
    private final SettingGroup sgFlight = settings.createGroup("Flight");

    List<EntityType<?>> list = new ArrayList<>();
    {
        Registries.ENTITY_TYPE.forEach(entityType -> {
            if (EntityUtils.isRideable(entityType) && entityType != EntityType.MINECART && entityType != EntityType.LLAMA && entityType != EntityType.TRADER_LLAMA) {
                list.add(entityType);
            }
        });
    }

    private final Setting<Set<EntityType<?>>> entities = sgControl.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Target entities.")
        .filter(entityType -> EntityUtils.isRideable(entityType) && entityType != EntityType.MINECART && entityType != EntityType.LLAMA && entityType != EntityType.TRADER_LLAMA)
        .defaultValue(list.toArray(new EntityType<?>[0]))
        .build()
    );

    private final Setting<Boolean> spoofSaddle = sgControl.add(new BoolSetting.Builder()
        .name("spoof-saddle*")
        .description("Lets you control rideable entities without them being saddled. Only works on older server versions.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> maxJump = sgControl.add(new BoolSetting.Builder()
        .name("max-jump")
        .description("Sets jump power to maximum.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> lockYaw = sgControl.add(new BoolSetting.Builder()
        .name("lock-yaw")
        .description("Locks the Entity's yaw.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cancelServerPackets = sgControl.add(new BoolSetting.Builder()
        .name("cancel-server-packets")
        .description("Cancels incoming vehicle move packets. WILL desync you from the server if you make an invalid movement.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> speed = sgSpeed.add(new BoolSetting.Builder()
        .name("speed")
        .description("Makes you go faster horizontally when riding entities.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> horizontalSpeed = sgSpeed.add(new DoubleSetting.Builder()
        .name("horizontal-speed")
        .description("Horizontal speed in blocks per second.")
        .defaultValue(10)
        .min(0)
        .sliderMax(50)
        .visible(speed::get)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgSpeed.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Use speed only when standing on a block.")
        .defaultValue(false)
        .visible(speed::get)
        .build()
    );

    private final Setting<Boolean> inWater = sgSpeed.add(new BoolSetting.Builder()
        .name("in-water")
        .description("Use speed when in water.")
        .defaultValue(true)
        .visible(speed::get)
        .build()
    );

    private final Setting<Boolean> flight = sgFlight.add(new BoolSetting.Builder()
        .name("fly")
        .description("Allows you to fly with entities.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> verticalSpeed = sgFlight.add(new DoubleSetting.Builder()
        .name("vertical-speed")
        .description("Vertical speed in blocks per second.")
        .defaultValue(6)
        .min(0)
        .sliderMax(20)
        .visible(flight::get)
        .build()
    );

    private final Setting<Double> fallSpeed = sgFlight.add(new DoubleSetting.Builder()
        .name("fall-speed")
        .description("How fast you will fall in blocks per second. Set to a small value to prevent fly kicks.")
        .defaultValue(0)
        .min(0)
        .visible(flight::get)
        .build()
    );

    private final Setting<Boolean> antiKick = sgFlight.add(new BoolSetting.Builder()
        .name("anti-fly-kick")
        .description("Whether to prevent the server from kicking you for flying.")
        .defaultValue(true)
        .visible(flight::get)
        .build()
    );

    private final Setting<Integer> delay = sgFlight.add(new IntSetting.Builder()
        .name("delay")
        .description("The amount of delay, in ticks, between flying down a bit and return to original position")
        .defaultValue(40)
        .min(1)
        .sliderMax(80)
        .visible(() -> flight.get() && antiKick.get())
        .build()
    );

    public EntityControl() {
        super(Categories.Movement, "entity-control", "Lets you control rideable entities without a saddle.", "entity-speed", "entity-fly", "boat-fly");
    }

    private int delayLeft;
    private double lastPacketY = Double.MAX_VALUE;
    private boolean sentPacket = false;

    @Override
    public void onActivate() {
        delayLeft = delay.get();
        sentPacket = false;
        lastPacketY = Double.MAX_VALUE;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (sentPacket && mc.player.getVehicle() != null) {
            VehicleMoveC2SPacket packet = VehicleMoveC2SPacket.fromVehicle(mc.player.getVehicle());
            ((IVec3d) packet.position()).meteor$setY(lastPacketY);
            mc.getNetworkHandler().sendPacket(packet);
            sentPacket = false;
        }

        delayLeft -= 1;
    }

    @EventHandler
    private void onEntityMove(EntityMoveEvent event) {
        Entity entity = event.entity;
        if (event.entity.getControllingPassenger() != mc.player || !entities.get().contains(entity.getType())) return;

        double velX = entity.getVelocity().x;
        double velY = entity.getVelocity().y;
        double velZ = entity.getVelocity().z;

        // Horizontal movement
        if (speed.get() && (!onlyOnGround.get() || entity.isOnGround() || entity.isFlyingVehicle()) && (inWater.get() || !entity.isTouchingWater())) {
            Vec3d vel = PlayerUtils.getHorizontalVelocity(horizontalSpeed.get());
            velX = vel.x;
            velZ = vel.z;
        }

        // Vertical movement
        if (flight.get()) {
            velY = 0;
            if (Input.isPressed(mc.options.jumpKey)) velY += verticalSpeed.get() / 20;
            if (Input.isPressed(mc.options.sprintKey)) velY -= verticalSpeed.get() / 20;
            else velY -= fallSpeed.get() / 20;
        }

        if (lockYaw.get()) entity.setYaw(mc.player.getYaw());
        ((IVec3d) event.movement).meteor$set(velX, velY, velZ);
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof VehicleMoveC2SPacket packet) || !antiKick.get()) return;

        double currentY = packet.position().y;
        if (delayLeft <= 0 && !sentPacket && shouldFlyDown(currentY) && EntityUtils.isOnAir(mc.player.getVehicle()) && !mc.player.getVehicle().isFlyingVehicle()) {
            ((IVec3d) packet.position()).meteor$setY(lastPacketY - 0.03130D);
            sentPacket = true;
            delayLeft = delay.get();
        }

        lastPacketY = currentY;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof VehicleMoveS2CPacket && cancelServerPackets.get()) {
            event.cancel();
        }
    }

    private boolean shouldFlyDown(double currentY) {
        if (currentY >= lastPacketY) return true;
        return lastPacketY - currentY < 0.03130D;
    }

    public boolean spoofSaddle() {
        return isActive() && spoofSaddle.get();
    }

    public boolean maxJump() {
        return isActive() && maxJump.get();
    }

    public boolean cancelJump() {
        if (!(mc.player.getVehicle() instanceof JumpingMount)) return false;
        return isActive() && entities.get().contains(mc.player.getVehicle().getType()) && flight.get();
    }
}
