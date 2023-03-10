/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.entity.EntityMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.vehicle.BoatEntity;
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

    private final Setting<Boolean> cancelBoatPaddle = sgGeneral.add(new BoolSetting.Builder()
        .name("cancel-boat-paddle")
        .description("Cancels Boat paddle.")
        .defaultValue(false)
        .build()
    );

    // Speed

    private final Setting<Boolean> entitySpeed = sgSpeed.add(new BoolSetting.Builder()
        .name("entity-speed")
        .description("Makes you go faster horizontally when riding entities.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> speed = sgSpeed.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Horizontal speed in blocks per second.")
        .defaultValue(10)
        .min(0)
        .sliderMax(50)
        .visible(entitySpeed::get)
        .build()
    );

    private final Setting<Boolean> onlyOnGround = sgSpeed.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("Use speed only when standing on a block.")
        .defaultValue(false)
        .visible(entitySpeed::get)
        .build()
    );

    private final Setting<Boolean> inWater = sgSpeed.add(new BoolSetting.Builder()
        .name("in-water")
        .description("Use speed only in water.")
        .defaultValue(false)
        .visible(entitySpeed::get)
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
        .defaultValue(0.625)
        .min(0)
        .visible(flight::get)
        .build()
    );

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
        if (entitySpeed.get() &&
            (!onlyOnGround.get() || entity.isOnGround()) && (inWater.get() || !entity.isTouchingWater())) {
                Vec3d vel = PlayerUtils.getHorizontalVelocity(speed.get());
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

    public boolean maxJump() {
        if (mc.player.getVehicle() == null) return false;
        return isActive() && entities.get().getBoolean(mc.player.getVehicle().getType()) && maxJump.get();
    }

    public boolean cancelBoatPaddle() {
        if (!(mc.player.getVehicle() instanceof BoatEntity boat)) return false;
        return isActive() && entities.get().getBoolean(boat.getType()) && cancelBoatPaddle.get();
    }

    public boolean cancelJump() {
        if (!(mc.player.getVehicle() instanceof AbstractHorseEntity horse)) return false;
        return isActive() && entities.get().getBoolean(horse.getType()) && flight.get();
    }

    public float getSaddledSpeed(float defaultSpeed) {
        return isActive() && entities.get().getBoolean(mc.player.getVehicle().getType()) && entitySpeed.get() ? speed.get().floatValue() : defaultSpeed;
    }
}
