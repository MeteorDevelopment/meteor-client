/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.entity.EntityMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.VehicleMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.c2s.play.VehicleMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import net.minecraft.util.math.Vec3d;

public class EntityControl extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpeed = settings.createGroup("Speed");
    private final SettingGroup sgFlight = settings.createGroup("Flight");

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Target entities.")
        .defaultValue(EntityType.BOAT, EntityType.CHEST_BOAT, EntityType.DONKEY, EntityType.HORSE, EntityType.MULE, EntityType.PIG, EntityType.SKELETON_HORSE, EntityType.STRIDER, EntityType.ZOMBIE_HORSE)
        .build()
    );

    private final Setting<Boolean> maxJump = sgGeneral.add(new BoolSetting.Builder()
        .name("max-jump")
        .description("Sets jump power to maximum.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> lockYaw = sgGeneral.add(new BoolSetting.Builder()
        .name("lock-yaw")
        .description("Locks the Entity's yaw.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> cancelServerPackets = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-server-packets")
        .description("Cancels incoming move packets.")
        .defaultValue(false)
        .build()
    );

    // Speed

    private final Setting<Boolean> speed = sgSpeed.add(new BoolSetting.Builder()
        .name("speed")
        .description("Makes you go faster horizontally when riding entities.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> horizontalSpeed = sgSpeed.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Horizontal speed in blocks per second.")
        .defaultValue(10)
        .min(0)
        .sliderMax(50)
        .visible(speed::get)
        .build()
    );

    private final Setting<Boolean> cancelBoatPaddle = sgSpeed.add(new BoolSetting.Builder()
        .name("cancel-boat-paddle")
        .description("Cancels Boat paddle.")
        .defaultValue(true)
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
        .description("Use speed only in water.")
        .defaultValue(false)
        .visible(speed::get)
        .build()
    );

    // Fly

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
        .description("How fast you will fall in blocks per second.")
        .defaultValue(0)
        .min(0)
        .visible(flight::get)
        .build()
    );

    private final Setting<Boolean> antiKick = sgFlight.add(new BoolSetting.Builder()
        .name("anti-kick")
        .description("Moves down occasionally to prevent floating kicks.")
        .defaultValue(true)
        .visible(flight::get)
        .build()
    );

    private final Setting<Integer> downDelay = sgFlight.add(new IntSetting.Builder()
        .name("delay")
        .description("How often do you move down while flying.")
        .defaultValue(15)
        .min(1)
        .sliderMax(200)
        .visible(() -> flight.get() && antiKick.get())
        .build()
    );

    private double lastPacketY;
    private int delayLeft;

    public EntityControl() {
        super(Categories.Movement, "entity-control", "Gives you more control over rideable entities.");
    }

    @EventHandler
    private void onEntityMove(EntityMoveEvent event) {
        Entity entity = event.entity;
        if (entity.getPrimaryPassenger() != mc.player || !entities.get().getBoolean(entity.getType())) return;

        double velX = entity.getVelocity().x;
        double velY = entity.getVelocity().y;
        double velZ = entity.getVelocity().z;

        // Horizontal movement
        if (speed.get() &&
            (!onlyOnGround.get() || entity.isOnGround()) && (inWater.get() || !entity.isTouchingWater())) {
                Vec3d vel = PlayerUtils.getHorizontalVelocity(horizontalSpeed.get());
                velX = vel.x;
                velZ = vel.z;
        }

        // Vertical movement
        if (flight.get()) {
            velY = 0;
            if (mc.options.jumpKey.isPressed()) velY += verticalSpeed.get() / 20;
            if (mc.options.sprintKey.isPressed()) velY -= verticalSpeed.get() / 20;
            else velY -= fallSpeed.get() / 20;
        }

        // Apply movement
        if (lockYaw.get()) entity.setYaw(mc.player.getYaw());
        ((IVec3d)event.entity.getVelocity()).set(velX, velY, velZ);
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof VehicleMoveS2CPacket && cancelServerPackets.get()) {
            event.cancel();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (delayLeft > 0) delayLeft--;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof VehicleMoveC2SPacket packet) || !antiKick.get()) return;

        double currentY = packet.getY();
        if (delayLeft <= 0 && shouldFlyDown(currentY) && EntityUtils.isOnAir(mc.player.getVehicle())) {
            // actual check is for >= -0.03125D, but we have to do a bit more than that
            // due to the fact that it's a bigger or *equal* to, and not just a bigger than
            ((VehicleMoveC2SPacketAccessor) packet).setY(lastPacketY - 0.03130D);
            delayLeft = downDelay.get();
        } else {
            lastPacketY = currentY;
        }
    }

    private boolean shouldFlyDown(double currentY) {
        if (currentY >= lastPacketY) {
            return true;
        } else return lastPacketY - currentY < 0.03130D;
    }

    public boolean maxJump() {
        if (mc.player.getVehicle() == null) return false;
        return isActive() && entities.get().getBoolean(mc.player.getVehicle().getType()) && maxJump.get();
    }

    public boolean cancelBoatPaddle() {
        if (!(mc.player.getVehicle() instanceof BoatEntity boat)) return false;
        return isActive() && entities.get().getBoolean(boat.getType()) && cancelBoatPaddle.get() && speed.get();
    }

    public boolean cancelJump() {
        if (!(mc.player.getVehicle() instanceof AbstractHorseEntity horse)) return false;
        return isActive() && entities.get().getBoolean(horse.getType()) && flight.get();
    }

    public float getSaddledSpeed(float defaultSpeed) {
        return isActive() && entities.get().getBoolean(mc.player.getVehicle().getType()) && speed.get() ? horizontalSpeed.get().floatValue() : defaultSpeed;
    }
}
